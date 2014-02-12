/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gnieh.entities

import java.util.UUID

import scala.collection.{immutable => im }
import scala.collection.mutable.{
  Map,
  HashMap,
  MultiMap,
  Set
}
import scala.concurrent.stm._

import scala.reflect._

/** The `EntityManager` is responsible for creating, storing and deleting the entities
 *  and associated components.
 *  Components `T` stored in the manager may be mutable, that's why it is stored as `Ref[T]`
 *  so that they can be managed by the stm library.
 *
 *  @author Lucas Satabin
 */
class EntityManager {

  private val _entities =
    TSet.empty[Entity]

  private val tags =
    TMap.empty[Entity, String]

  private val ids =
    TMap.empty[String, TSet[Entity]]

  private val components =
    TMap.empty[Class[_ <: Component], TMap[Entity, TSet[Ref[Component]]]]

  def createSimple(): Entity = atomic { implicit txn =>
    val id = UUID.randomUUID.toString
    // add to managed entities
    _entities += id
    id
  }

  def createTagged(tag: String): Entity = atomic { implicit txn =>
    val id = UUID.randomUUID.toString
    // add id -> tag mapping
    tags(id) = tag
    // add tag -> id mapping
    ids.get(tag) match {
      case Some(entities) => entities += id
      case None           => ids(tag) = TSet(id)
    }
    // add to managed entities
    _entities += id
    id
  }

  def deleteEntity(entity: Entity): Unit = atomic { implicit txn =>
    tags.remove(entity).foreach(tag => ids(tag) -= entity)
    _entities -= entity
  }

  def addComponent(entity: Entity, component: Component): entity.type = atomic { implicit txn =>
    // get the store for this component
    val clazz = component.getClass
    val store = components.get(clazz).getOrElse(TMap.empty)
    store.get(entity) match {
      case Some(components) => components += Ref(component)
      case None             => store(entity) = TSet(Ref(component))
    }
    if(!components.contains(clazz))
      components(clazz) = store
    entity
  }

  def hasComponentType[T <: Component: ClassTag](entity: Entity): Boolean = atomic { implicit txn =>
    // the entity is in the component store and the list of components is not empty
    // this means that the entity has the given component
    for(store <- components.get(implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[Component]]))
      yield store.contains(entity) && store(entity).nonEmpty
  }.getOrElse(false)

  def hasComponent(entity: Entity, component: Component): Boolean = atomic { implicit txn =>
    for(store <- components.get(component.getClass))
      yield store.contains(entity) && store(entity).contains(Ref(component))
  }.getOrElse(false)

  def getComponent[T <: Component: ClassTag](entity: Entity): Option[Ref[T]] = atomic { implicit txn =>
    (for(store <- components.get(implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[Component]]))
      yield store(entity).headOption.collect { case t => t.asInstanceOf[Ref[T]] }).flatten
  }

  def removeComponentType[T <: Component: ClassTag](entity: Entity): Unit = atomic { implicit txn =>
    val clazz = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[Component]]
    for(store <- components.get(clazz)) {
      store -= entity
      if(store.isEmpty)
        components -= clazz
    }
  }

  def removeComponent(entity: Entity, component: Component): Unit = atomic { implicit txn =>
    val clazz = component.getClass
    for(store <- components.get(clazz)) {
      store -= entity
      if(store.isEmpty)
        components -= clazz
    }
  }

  // an entity manager is also a collection of entities
  def entities: im.Set[Entity] =
    _entities.single.toSet

  def entities(tag: String): im.Set[Entity] =
    ids.single.get(tag).getOrElse(TSet()).single.toSet

}


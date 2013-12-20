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

class EntityManager {

  private val _entities =
    TSet.empty[Entity]

  private val tags =
    TMap.empty[Entity, String]

  private val ids =
    TMap.empty[String, TSet[Entity]]

  private val components =
    TMap.empty[Class[_ <: Component], TMap[Entity, TSet[Component]]]

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
      case Some(components) => components += component
      case None             => store(entity) = TSet(component)
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
      yield store.contains(entity) && store(entity).contains(component)
  }.getOrElse(false)

  def getComponent[T <: Component: ClassTag](entity: Entity): Option[T] = atomic { implicit txn =>
    for(store <- components.get(implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[Component]]))
      yield store(entity).headOption.collect { case t: T => t }
  }.flatten

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


package gnieh.entities

import java.util.UUID

import scala.collection.{immutable => im }
import scala.collection.mutable.{
  Map,
  HashMap,
  MultiMap,
  Set
}

class EntityManager {

  private val _entities: Set[Entity] =
    Set.empty

  private val tags: Map[Entity, String] =
    Map.empty

  private val ids: MultiMap[String, Entity] =
    new HashMap[String, Set[Entity]] with MultiMap[String, Entity]

  private val components: Map[Class[_ <: Component], MultiMap[Entity, Component]] =
    Map.empty.withDefaultValue(new HashMap[Entity, Set[Component]] with MultiMap[Entity, Component])

  def createSimple(): Entity = {
    val id = UUID.randomUUID.toString
    // add to managed entities
    _entities += id
    id
  }

  def createTagged(tag: String): Entity = {
    val id = UUID.randomUUID.toString
    // add id -> tag mapping
    tags(id) = tag
    // add tag -> id mapping
    ids.addBinding(tag, id)
    // add to managed entities
    _entities += id
    id
  }

  def addComponent(entity: Entity, component: Component): entity.type = {
    // get the store for this component
    val clazz = component.getClass
    val store = components(clazz)
    store.addBinding(entity, component)
    if(!components.contains(clazz))
      components(clazz) = store
    entity
  }

  def hasComponentType[T <: Component: Manifest](entity: Entity): Boolean = {
    val store = components(implicitly[Manifest[T]].runtimeClass.asInstanceOf[Class[Component]])
    // the entity is in the component store and the list of components is not empty
    // this means that the entity has the given component
    store.contains(entity) && store(entity).nonEmpty
  }

  def hasComponent(entity: Entity, component: Component): Boolean = {
    val store = components(component.getClass)
    store.contains(entity) && store(entity).contains(component)
  }

  def getComponent[T <: Component: Manifest](entity: Entity): Option[T] = {
    val store = components(implicitly[Manifest[T]].runtimeClass.asInstanceOf[Class[Component]])
    store(entity).headOption.collect { case t: T => t }
  }

  // an entity manager is also a collection of entities
  def entities: im.Set[Entity] =
    _entities.toSet

}


package gnieh

import scala.concurrent.stm._

package object entities {

  type Entity = String

  implicit class RichEntity(val entity: Entity) extends AnyVal {

    /** Adds the given component to the entity */
    def add(component: Component)(implicit manager: EntityManager): entity.type =
      manager.addComponent(entity, component)

    /** Removes the given component to the entity */
    def remove(component: Component)(implicit manager: EntityManager): Unit =
      manager.removeComponent(entity, component)

    /** Removes all components of a given type attached to the entity */
    def remove[T <: Component: Manifest](implicit manager: EntityManager): Unit =
      manager.removeComponentType[T](entity)

    /** Indicates whether the entity has at least one component of the given type */
    def has[T <: Component: Manifest](implicit manager: EntityManager): Boolean =
      manager.hasComponentType[T](entity)

    /** Indicates whether the entity has the given component */
    def has(component: Component)(implicit manager: EntityManager): Boolean =
      manager.hasComponent(entity, component)

    /** Gets the component of the given type attached to the entity if any */
    def get[T <: Component: Manifest](implicit manager: EntityManager): Option[Ref[T]] =
      manager.getComponent[T](entity)

    /** Removes the entity from the system */
    def delete(implicit manager: EntityManager): Unit =
      manager.deleteEntity(entity)

  }

}

package gnieh

package object entities {

  type Entity = String

  implicit class RichEntity(val entity: Entity) extends AnyVal {

    def add(component: Component)(implicit manager: EntityManager): entity.type =
      manager.addComponent(entity, component)

    def has[T <: Component: Manifest](implicit manager: EntityManager): Boolean =
      manager.hasComponentType[T](entity)

    def has(component: Component)(implicit manager: EntityManager): Boolean =
      manager.hasComponent(entity, component)

    def get[T <: Component: Manifest](implicit manager: EntityManager): Option[T] =
      manager.getComponent[T](entity)

  }

}

package gnieh.entities

abstract class System {

  def process(delta: Long)(implicit manager: EntityManager): Unit

}


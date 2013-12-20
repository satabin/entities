package gnieh.entities

import scala.reflect._

private[entities] case object Tick
private[entities] case class Process(time: Long, manager: EntityManager)
private[entities] case class Publish[T](event: T, tag: ClassTag[T])


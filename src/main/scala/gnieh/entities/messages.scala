package gnieh.entities

import scala.reflect._

// ========== public messages sent to the engine to interact with it ==========
case object StartEngine
case object StopEngine

// ========== private messages used internally by the engine and the systems to communicate ==========
private[entities] case object Tick
private[entities] case class Process(time: Long, manager: EntityManager)
private[entities] case class Publish[T](origin: Entity, event: T, tag: ClassTag[T])


package gnieh.entities

import scala.compat.Platform
import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor.{
  Actor,
  ActorRef,
  Cancellable,
  PoisonPill,
  Props
}
import akka.pattern.ask
import akka.util.Timeout

import scala.reflect.ClassTag

/** A complete entity engine that manages its own entitie at a specific tick.
 *  It also provides systems in this engine with an event bus, so that systems may publish
 *  and subscribe to events.
 *
 *  @author Lucas Satabin
 */
abstract class Engine(tick: FiniteDuration) extends Actor {

  implicit val manager = new EntityManager

  implicit val timeout = Timeout(tick)

  import context.dispatcher

  def receive = stopped

  def started(ticks: Cancellable): Receive = {
    case Tick =>
      // standard tick event increments time
      chain(Process(Platform.currentTime, manager), systems)

    case pub @ Publish(_, _) =>
      // broadcast to all child systems
      // this is not done in the synchronous part, hence time does not change
      context.actorSelection("system::*") ! pub

    case Process(time, _) =>
      // TODO better logging
      println(s"End of processing for timestamp $time")

    case StopEngine | PoisonPill =>
      context.become(stopped)
      ticks.cancel()

  }

  val stopped: Receive = {
    case StartEngine =>
      context.become(started(context.system.scheduler.schedule(Duration.Zero, tick, self, Tick)))
  }

  /** Creates a system managed by this engine */
  def system[T <: EntitySystem: ClassTag](make: EntityManager => T): ActorRef =
    context.actorOf(Props(make(manager)), name = s"system::$nextId")

  def systems: List[ActorRef]

  // ========== internals ==========

  private var currentId = 0

  private def nextId: Int = {
    val res = currentId
    currentId += 1
    res
  }

  private def chain(process: Process, systems: List[ActorRef]): Unit = systems match {
    case ref :: rest =>
      for(_ <- ref.ask(process))
        chain(process, rest)
    case Nil =>
      // send to self to notify end of processing for this tick
      self ! process
  }

}


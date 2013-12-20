package gnieh.entities

import scala.compat.Platform
import scala.concurrent.duration._

import akka.actor.{
  Actor,
  ActorRef,
  Cancellable,
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

  private var scheduled: Option[Cancellable] = None

  override def preStart(): Unit = {
    // upon start we schedule the tick messages
    scheduled =
      Option(context.system.scheduler.schedule(Duration.Zero, tick, self, Tick))
  }

  override def postStop(): Unit = {
    // upon start we cancel the tick task
    scheduled.foreach(_.cancel())
  }

  def receive = {
    case Tick =>
      // standard tick event increments time
      chain(Platform.currentTime, systems)

    case pub @ Publish(_, _) =>
      // broadcast to all child systems
      // this is not done in the synchronous part, hence time does not change
      context.actorSelection("system::*") ! pub

  }

  /** Creates a system managed by this engine */
  def system[T <: System: ClassTag](make: =>T): ActorRef =
    context.actorOf(Props(make), name = s"system::$nextId")

  val systems: List[ActorRef]

  // ========== internals ==========

  private var currentId = 0

  private def nextId: Int = {
    val res = currentId
    currentId += 1
    res
  }

  private def chain(time: Long, systems: List[ActorRef]): Unit = systems match {
    case ref :: rest =>
      for(true <- ref.ask(Process(time, manager)).mapTo[Boolean])
        chain(time, rest)
    case Nil =>
      ()
  }

}


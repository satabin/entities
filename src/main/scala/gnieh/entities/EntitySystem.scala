package gnieh.entities

import scala.compat.Platform

import akka.actor.Actor

import scala.reflect._

import scala.concurrent.stm._

import scala.collection.mutable.{
  HashMap,
  MultiMap,
  Set
}

/** A system trigeers computaion for each tick and is also responsible for
 *  reacting to asynchronous events it registered to
 *
 *  @author Lucas Satabin
 */
abstract class EntitySystem(val manager: EntityManager) extends Actor {

  protected implicit val _manager = manager

  private val subscriptions: MultiMap[ClassTag[_], Subscription[_]] =
    new HashMap[ClassTag[_], Set[Subscription[_]]] with MultiMap[ClassTag[_], Subscription[_]]

  def receive =
    behavior(Platform.currentTime)

  def behavior(last: Long): Receive = {
    case Process(time, manager) if last < time =>
      // last processing was done in the past
      context.become(behavior(time))
      atomic { implicit txn =>
        // the call to process is wrapped in an atomic block
        // because it uses a shared resource : the entity manager.
        // entities may be added/deleted/altered by a system, to ensure we are
        // always working with a consistent state of the entity manager, and that
        // all modifications are either committed or discarded, we use stm
        process(time - last)
      }
      // yes the message was processed
      sender ! true

    case Process(_, _) =>
      // processing time is in the past compared to the last registered time
      // just skip this tick, as we already processed something in the future
      // this can occur when a tick took particularly long to compute and the next one
      // is much quicker
      sender ! false

    case Publish(event, tag) if subscriptions.contains(tag) =>
      // receive an event to which type we subscribed
      for(subscription <- subscriptions(tag))
        atomic { implicit txn =>
          // events are processed asynchronously and do not change last processing time.
          // however, they can interfer with the entities and are thus wrapped in a transaction
          subscription(event, manager)
        }

  }

  def publish[T: ClassTag](event: T): Unit =
    context.parent ! Publish(event, implicitly[ClassTag[T]])

  def subscribe[T: ClassTag](react: T => EntityManager => Unit): Unit =
    subscriptions.addBinding(implicitly[ClassTag[_]], new Subscription(react))

  def unsubscribe[T: ClassTag]: Unit =
    subscriptions -= implicitly[ClassTag[_]]

  /** Processing of a tick by this system. The framework ensures that this
   *  method is called only if the tick appears the future since the last
   *  processing.
   *  It is also called inside a transaction to ensure the user to have a
   *  consistent view of the entire entity system.
   */
  def process(delta: Long)(implicit txn: InTxn): Unit

}

private class Subscription[T: ClassTag](val handler: T => EntityManager => Unit) {
  def apply(value: Any, manager: EntityManager): Unit = value match {
    case v: T => handler(v)(manager)
    case _    =>
  }
}


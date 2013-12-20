package gnieh.entities

import scala.compat.Platform

import akka.actor.Actor

import scala.reflect._

import scala.collection.mutable.{
  HashMap,
  MultiMap,
  Set
}

abstract class System(implicit val manager: EntityManager) extends Actor {

  private val subscriptions: MultiMap[ClassTag[_], Subscription[_]] =
    new HashMap[ClassTag[_], Set[Subscription[_]]] with MultiMap[ClassTag[_], Subscription[_]]

  def receive =
    behavior(Platform.currentTime)

  def behavior(last: Long): Receive = {
    case Process(time, manager) if last <= time =>
      // last processing was done in the past
      context.become(behavior(time))
      process(time - last)
      sender ! true
    case Process(_, _) =>
      // processing time is in the past compared to the last registered time
      sender ! false
    case Publish(event, tag) if subscriptions.contains(tag) =>
      // receive an event to which type we subscribed
      for(handler <- subscriptions(tag))
        handler(event)
  }

  def publish[T: ClassTag](event: T): Unit =
    context.parent ! Publish(event, implicitly[ClassTag[T]])

  def subscribe[T: ClassTag](react: T => Unit): Unit =
    subscriptions.addBinding(implicitly[ClassTag[_]], new Subscription(react))

  def unsubscribe[T: ClassTag]: Unit =
    subscriptions -= implicitly[ClassTag[_]]

  def process(delta: Long): Unit

}

private class Subscription[T: ClassTag](val handler: T => Unit) {
  def apply(value: Any): Unit = value match {
    case v: T => handler(v)
    case _    =>
  }
}


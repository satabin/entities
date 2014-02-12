/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    case Process(time) if last < time =>
      // last processing was done in the past
      atomic { implicit txn =>
        // the call to process is wrapped in an atomic block
        // because it uses a shared resource : the entity manager.
        // entities may be added/deleted/altered by a system, to ensure we are
        // always working with a consistent state of the entity manager, and that
        // all modifications are either committed or discarded, we use stm
        process(time - last)
      }
      context.become(behavior(time))
      // yes the message was processed
      sender ! true

    case Process(_) =>
      // processing time is in the past compared to the last registered time
      // just skip this tick, as we already processed something in the future
      // this can occur when a tick took particularly long to compute and the next one
      // is much quicker
      sender ! false

    case Publish(origin, event, tag) if subscriptions.contains(tag) =>
      // receive an event to which type we subscribed
      for(subscription <- subscriptions(tag))
        atomic { implicit txn =>
          // events are processed asynchronously and do not change last processing time.
          // however, they can interfer with the entities and are thus wrapped in a transaction
          subscription(origin, event, txn)
        }

  }

  def publish[T: ClassTag](origin: Entity, event: T): Unit =
    context.parent ! Publish(origin, event, implicitly[ClassTag[T]])

  def subscribe[T: ClassTag](react: (Entity, T) => InTxn => Unit): Unit =
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

private class Subscription[T: ClassTag](val handler: (Entity, T) => InTxn => Unit) {
  def apply(origin: Entity, value: Any, txn: InTxn): Unit = value match {
    case v: T => handler(origin, v)(txn)
    case _    =>
  }
}


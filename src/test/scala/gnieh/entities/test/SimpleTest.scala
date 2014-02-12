package gnieh.entities
package test

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.gracefulStop
import akka.testkit._

import scala.concurrent.stm._

import org.scalatest.FunSuiteLike

case class Velocity(v: Int) extends Component

case class EchoSystem(id: Int)(manager: EntityManager) extends EntitySystem(manager) {

  def process(delta: Long)(implicit txn: InTxn): Unit = {
    println(s"processing system $id after delta $delta")
    for {
      entity <- manager.entities
      v <- entity.get[Velocity]
      if v().v > 10
    } yield v() = Velocity(4)
    Thread.sleep(10)
  }

}

class TestEngine extends Engine(20.milliseconds) {

  val systems = List(
    system(EchoSystem(1)),
    system(EchoSystem(2)),
    system(EchoSystem(3)),
    system(EchoSystem(4))
  )

}

class SimpleSpec extends TestKit(ActorSystem("test-entities")) with FunSuiteLike {

  val engine = system.actorOf(Props[TestEngine], "entity-manager")

  test("sequenced systems") {
    engine ! StartEngine
    try {
      expectNoMsg(100.milliseconds)
    } finally {
      import system.dispatcher
      gracefulStop(engine, 5.seconds, StopEngine).onComplete { _ =>
        system.shutdown()
        println("stopped")
      }
    }
  }

}


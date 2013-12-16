package gnieh.entities

import java.util.UUID

trait Component {

  val id: String = UUID.randomUUID.toString

  override def toString =
    s"Component [$id: $getClass]"

}


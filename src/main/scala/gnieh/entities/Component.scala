package gnieh.entities

import java.util.UUID

/** A component can be attached to any entity, any number of times
 *
 *  @author Lucas Satabin
 */
trait Component {

  val id: String = UUID.randomUUID.toString

  override def toString =
    s"Component [$id: $getClass]"

}


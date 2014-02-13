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
package tutorial

import scala.concurrent.stm._

/** Compute the new position of an entity according to its speed */
class MovementSystem(manager: EntityManager) extends EntitySystem(manager) {

  def process(delta: Long)(implicit txn: InTxn): Unit =
    for {
      // for each entity
      entity <- manager.entities
      // having a position component
      position <- entity.get[Position]
      // and a velocity component
      velocity <- entity.get[Velocity]
    } {
      // update its position according to the speed
      position().x += velocity().dx * delta
      position().y += velocity().dy * delta
    }

}

/** Compute the new velocity vector according to the rotation of the entity */
class RotationSystem(manager: EntityManager) extends EntitySystem(manager) {

  private var step = 1

  def process(delta: Long)(implicit txn: InTxn): Unit = {
    for {
      // for each entity
      entity <- manager.entities
      // having a rotation component
      rotation <- entity.get[Rotation]
      // and a velocity component
      velocity <- entity.get[Velocity]
    } {
      // update the velocity according to the rotation angle
      velocity().dx = math.cos(step * rotation().angle)
      velocity().dy = math.sin(step * rotation().angle)
    }
    // increment the number of steps performed
    step += 1
  }

}


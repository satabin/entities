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
package gnieh

import scala.concurrent.stm._

package object entities {

  type Entity = String

  implicit class RichEntity(val entity: Entity) extends AnyVal {

    /** Adds the given component to the entity */
    def add(component: Component)(implicit manager: EntityManager): entity.type =
      manager.addComponent(entity, component)

    /** Removes the given component to the entity */
    def remove(component: Component)(implicit manager: EntityManager): Unit =
      manager.removeComponent(entity, component)

    /** Removes all components of a given type attached to the entity */
    def remove[T <: Component: Manifest](implicit manager: EntityManager): Unit =
      manager.removeComponentType[T](entity)

    /** Indicates whether the entity has at least one component of the given type */
    def has[T <: Component: Manifest](implicit manager: EntityManager): Boolean =
      manager.hasComponentType[T](entity)

    /** Indicates whether the entity has the given component */
    def has(component: Component)(implicit manager: EntityManager): Boolean =
      manager.hasComponent(entity, component)

    /** Gets the component of the given type attached to the entity if any */
    def get[T <: Component: Manifest](implicit manager: EntityManager): Option[Ref[T]] =
      manager.getComponent[T](entity)

    /** Removes the entity from the system */
    def delete(implicit manager: EntityManager): Unit =
      manager.deleteEntity(entity)

  }

}

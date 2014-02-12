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

import scala.reflect._

// ========== public messages sent to the engine to interact with it ==========
case object StartEngine
case object StopEngine

// ========== private messages used internally by the engine and the systems to communicate ==========
private[entities] case object Tick
private[entities] case class Process(time: Long, manager: EntityManager)
private[entities] case class Publish[T](origin: Entity, event: T, tag: ClassTag[T])


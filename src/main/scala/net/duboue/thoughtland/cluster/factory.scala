/*
 *   This file is part of Thoughtland -- Verbalizing n-dimensional objects.
 *   Copyright (C) 2013 Pablo Duboue <pablo.duboue@gmail.com>
 * 
 *   Thoughtland is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as 
 *   published by the Free Software Foundation, either version 3 of 
 *   the License, or (at your option) any later version.
 *
 *   Meetdle is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *   
 *   You should have received a copy of the GNU Affero General Public 
 *   License along with Thoughtland.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.duboue.thoughtland.cluster

import net.duboue.thoughtland.Clusterer
import net.duboue.thoughtland.cluster.mahout.MahoutClusterer

object ClustererEngine extends Enumeration {
  type ClustererEngine = ClustererEngineVal

  case class ClustererEngineVal(name: String, make: () => Clusterer) extends Val(name)

  val Mahout = ClustererEngineVal("mahout", { () => new MahoutClusterer() })

  implicit def valueToClustererEngine(v: Value): ClustererEngineVal = v.asInstanceOf[ClustererEngineVal]
}

object ClustererFactory {
  def apply(engine: ClustererEngine.ClustererEngine) = engine.make()
  def apply(engine: String): Clusterer = ClustererEngine.withName(engine.toLowerCase()).make()
}
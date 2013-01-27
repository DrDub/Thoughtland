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

package net.duboue.thoughtland

/**
 * Analysis results
 */

object RelativeMagnitude extends Enumeration {
  type RelativeMagnitude = RelativeMagnitudeVal

  case class RelativeMagnitudeVal(name: String) extends Val(name);

  val VeryBig = RelativeMagnitudeVal("VeryBig")
  val Big = RelativeMagnitudeVal("Big")
  val Medium = RelativeMagnitudeVal("Medium")
  val Small = RelativeMagnitudeVal("Small")
  val VerySmall = RelativeMagnitudeVal("verySmall")

  implicit def valueToRelativeMagnitude(v: Value): RelativeMagnitudeVal = v.asInstanceOf[RelativeMagnitudeVal]
}

import RelativeMagnitude._

case class ComponentDensity(component: Int, density: RelativeMagnitude.RelativeMagnitude) extends Finding
case class ComponentSize(component: Int, size: RelativeMagnitude.RelativeMagnitude) extends Finding
case class ComponentDistance(c1: Int, c2: Int, distance: RelativeMagnitude.RelativeMagnitude) extends Finding


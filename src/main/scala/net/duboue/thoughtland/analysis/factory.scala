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

package net.duboue.thoughtland.analysis

import net.duboue.thoughtland.ComponentAnalyzer
import net.duboue.thoughtland.analysis.basic.BasicAnalyzer

object AnalyzerEngine extends Enumeration {
  type AnalyzerEngine = AnalyzerEngineVal

  case class AnalyzerEngineVal(name: String, make: () => ComponentAnalyzer) extends Val(name)

  val Basic = AnalyzerEngineVal("basic", { () => new BasicAnalyzer() })

  implicit def valueToAnalyzerEngine(v: Value): AnalyzerEngineVal = v.asInstanceOf[AnalyzerEngineVal]
}

object AnalyzerFactory {
  def apply(engine: AnalyzerEngine.AnalyzerEngine) = engine.make()
  def apply(engine: String): ComponentAnalyzer = AnalyzerEngine.withName(engine.toLowerCase()).make()
}
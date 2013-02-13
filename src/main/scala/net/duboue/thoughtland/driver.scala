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

import net.duboue.thoughtland.cloud.CloudExtractorFactory
import net.duboue.thoughtland.cluster.ClustererFactory
import net.duboue.thoughtland.analysis.AnalyzerFactory
import net.duboue.thoughtland.nlg.GeneratorFactory

object ThoughtlandDriver {
  val default = Seq("wekaerror", "mahout", "basic", "simplenlg")
  def apply(components: String*): Thoughtland = components match {
    case Seq("default") => apply(default: _*)
    case Seq(clouder, clusterer, analyzer, generator) =>
      Thoughtland(CloudExtractorFactory(clouder),
        ClustererFactory(clusterer),
        AnalyzerFactory(analyzer),
        GeneratorFactory(generator))
  }

}
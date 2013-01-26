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

import java.net.URI

/**
 * Types for Thoughtland.
 */

case class TrainingData(uri: URI)

case class CloudPoints(points: Array[Array[Double]])

case class Component(center: Array[Double], radii: Array[Double])
case class Components(main: Component, parts: Array[Component])

abstract class Relation; // see relations.scala for the actual relations
case class Analysis(rels: Array[Relation])

case class GeneratedText(paras: Array[Paragraph])
case class Paragraph(sent: Array[Sentence])
case class Sentence(text: String)

case class Config(randomSeed: Long, storeResults: Boolean)
case class Environment(resultsDir: java.io.File, tmpDir: java.io.File, config: Config)


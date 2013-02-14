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

case class Component(center: Array[Double], radii: Array[Double], coveredPoints: Long)
case class Components(main: Component, parts: List[Component])

abstract class Finding; // see findings.scala for the actual findings
case class Analysis(numberOfComponents: Int, numberOfDimensions: Int, findings: List[Finding])

case class GeneratedText(paras: List[Paragraph]) {
  override def toString = paras.mkString("", "\n", "\n")
}
case class Paragraph(sent: List[Sentence]) {
  override def toString = sent.mkString("", " ", "")
}
case class Sentence(text: String) {
  override def toString = text
}

case class Config(randomSeed: Long, storeResults: Boolean)
case class Environment(resultsDir: java.io.File, tmpDir: java.io.File, config: Config)


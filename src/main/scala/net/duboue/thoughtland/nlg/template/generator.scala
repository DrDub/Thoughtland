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

package net.duboue.thoughtland.nlg.template

import net.duboue.thoughtland.Analysis
import net.duboue.thoughtland.ComponentDensity
import net.duboue.thoughtland.ComponentDistance
import net.duboue.thoughtland.ComponentSize
import net.duboue.thoughtland.Environment
import net.duboue.thoughtland.Finding
import net.duboue.thoughtland.GeneratedText
import net.duboue.thoughtland.Generator
import net.duboue.thoughtland.Paragraph
import net.duboue.thoughtland.Sentence
import net.duboue.thoughtland.RelativeMagnitude
import net.duboue.thoughtland.nlg.BasicVerbalizations

class TemplateGenerator extends Generator with BasicVerbalizations {

  def thereAre(these: Int, ofThat: String): Sentence =
    Sentence(these match {
      case 1 => s"There is one $ofThat."
      case x => s"There are ${numToStr(x)} ${ofThat}s."
    })

  def firstParagraph(analysis: Analysis): Paragraph =
    Paragraph(List(thereAre(analysis.numberOfComponents, "component"),
      thereAre(analysis.numberOfDimensions, "dimension")))

  def findingToSentence(finding: Finding) = Sentence(
    finding match {
      case ComponentDensity(c, d) => s"Component ${numToStr(c)} is ${densityToStr(d)}."
      case ComponentSize(c, d) => s"Component ${numToStr(c)} is ${sizeToStr(d)}."
      case ComponentDistance(c1, c2, d) => s"Component ${numToStr(c1)} is ${distanceToStr(d)} Component ${numToStr(c2)}."
    })

  def apply(analysis: Analysis)(implicit env: Environment): GeneratedText =
    GeneratedText(List(firstParagraph(analysis),
      Paragraph(analysis.findings.map(findingToSentence))))
}


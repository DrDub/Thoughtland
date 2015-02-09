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

package net.duboue.thoughtland.nlg.simplenlg

import org.junit._
import net.duboue.thoughtland.Analysis
import net.duboue.thoughtland.ComponentDensity
import net.duboue.thoughtland.ComponentDistance
import net.duboue.thoughtland.ComponentSize
import net.duboue.thoughtland.RelativeMagnitude
import net.duboue.thoughtland.Environment
import java.io.File
import net.duboue.thoughtland.Config
import Assert._
import net.duboue.thoughtland.nlg.template.TemplateGenerator

@Test
class SimpleNlgGeneratorTest {
  @Test
  def testSimpleNlgGeneration() = {
    import RelativeMagnitude._
    val analysis = Analysis(3, 8, List(ComponentSize(0, Small), ComponentDensity(0, VeryBig),
      ComponentDistance(0, 1, Big), ComponentDistance(0, 2, Big), ComponentSize(1, Small),
      ComponentDensity(1, VeryBig), ComponentDistance(1, 2, Medium), ComponentSize(2, VeryBig)))
    implicit val env = Environment(null, null, Config(1L, false, false))
    val generator = new SimpleNlgGenerator
    val generatedText = generator(analysis)
    assertEquals("There are three components and eight dimensions.", generatedText.paras(0).sent(0).text)
    System.out.println(generatedText);
    System.out.println((new TemplateGenerator)(analysis))
  }

  @Test
  def testSimpleNlgGeneration2() = {
    import RelativeMagnitude._
    val analysis = Analysis(4, 8, List(ComponentSize(0, Small), ComponentDensity(0, VeryBig),
      ComponentDistance(0, 1, Big), ComponentDistance(0, 2, Big), ComponentSize(1, Small),
      ComponentDensity(1, VeryBig), ComponentDistance(1, 2, Medium), 
      ComponentDistance(1, 3, Big),
      ComponentDistance(0, 3, Big),
      ComponentSize(2, VeryBig)))
    implicit val env = Environment(null, null, Config(1L, false, false))
    val generator = new SimpleNlgGenerator
    val generatedText = generator(analysis)
    System.out.println(generatedText);
    assertEquals("Components 2 and 3 are at a good distance from each other.", generatedText.paras(0).sent(3).text)
  }
}
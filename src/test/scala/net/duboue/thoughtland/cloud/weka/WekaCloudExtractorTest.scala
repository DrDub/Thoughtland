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

package net.duboue.thoughtland.cloud.weka

import org.junit._
import Assert._
import java.io.File
import net.duboue.thoughtland.TrainingData
import weka.classifiers.functions.MultilayerPerceptron
import net.duboue.thoughtland.Environment
import net.duboue.thoughtland.Config

@Test
class WekaCloudExtractorTest {

  @Test
  def testCloudGeneration = {
    val stream = classOf[WekaCloudExtractorTest].getResourceAsStream("auto-mpg.arff")
    val arff = File.createTempFile("auto-mpg", ".arff")
    arff.deleteOnExit()
    val extractor = new WekaCloudExtractor()
    implicit val env = Environment(new File("."), new File("/tmp"), Config(1L, false))
    val points = extractor(TrainingData(arff.toURI()), classOf[MultilayerPerceptron].getName(), Array("-H", "9,4")).points
  }

}
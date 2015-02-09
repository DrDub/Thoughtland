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

import java.io.File

import org.junit._

import net.duboue.thoughtland.Config
import net.duboue.thoughtland.Environment
import net.duboue.thoughtland.TrainingData
import weka.classifiers.functions.MultilayerPerceptron

@Test
class WekaCloudExtractorTest {

  @Test
  def testCloudGeneration() = {
    val stream = new java.io.BufferedReader(new java.io.InputStreamReader(classOf[WekaCloudExtractorTest].getResourceAsStream("auto-mpg.arff")))
    val arff = File.createTempFile("auto-mpg", ".arff")
    arff.deleteOnExit()
    val pw = new java.io.PrintWriter(arff) // something less java-ey will be good here
    try {
      var line = stream.readLine()
      while (line != null) {
        pw.println(line)
        line = stream.readLine()
      }
    } finally {
      stream.close()
      pw.close()
    }

    val extractor = new WekaCloudExtractor()
    implicit val env = Environment(new File("."), new File("/tmp"), Config(1L, false, false))
    val points = extractor(TrainingData(arff.toURI()), classOf[MultilayerPerceptron].getName(), Array("-c", "0", "-H", "3,2")).points
    val pointsPW = new java.io.PrintWriter(new java.io.File("/tmp/points2.csv"))
    try {
      for (vector <- points) {
        pointsPW.println(vector.toList.mkString("", ", ", ""))
      }
    } finally {
      pointsPW.close()
    }
  }

}
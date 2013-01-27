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

package net.duboue.thoughtland.cluster.mahout

import java.io.File
import org.junit._
import Assert._
import net.duboue.thoughtland.Config
import net.duboue.thoughtland.Environment
import net.duboue.thoughtland.TrainingData
import weka.classifiers.functions.MultilayerPerceptron
import net.duboue.thoughtland.cloud.CloudExtractorFactory
import net.duboue.thoughtland.cloud.FileEngine
import net.duboue.thoughtland.TrainingData
import net.duboue.thoughtland.cluster.ClustererFactory
import net.duboue.thoughtland.cluster.MahoutEngine

@Test
class MahoutClustererTest {
  @Test
  def testMahoutClustering = {
    val tmpDir = new File(File.createTempFile("test", "mahout").getAbsolutePath() + ".dir")
    assertTrue(tmpDir.mkdirs())
    implicit val env = Environment(new File("."), tmpDir, Config(1L, false))
    val cloud = CloudExtractorFactory(FileEngine()).apply(TrainingData(classOf[MahoutClustererTest].getResource("auto-mpg-points2.csv").toURI), "", Array())
    assertEquals(397, cloud.points.length)
    val components = ClustererFactory(MahoutEngine()).apply(cloud, 1000)
    System.out.println(components)
    System.out.println(components.parts.length)
    System.out.println(components.main.center.toList)
    for (component <- components.parts)
      System.out.println(component.center.mkString("", ", ", ""))
  }
}
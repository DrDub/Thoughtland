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
import scala.collection.JavaConversions._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.IntWritable
import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.io.Text
import org.apache.mahout.clustering.dirichlet.DirichletDriver
import org.apache.mahout.clustering.dirichlet.models.DistributionDescription
import org.apache.mahout.clustering.dirichlet.models.GaussianCluster
import org.apache.mahout.clustering.dirichlet.models.GaussianClusterDistribution
import org.apache.mahout.clustering.iterator.ClusterWritable
import org.apache.mahout.common.iterator.sequencefile.PathFilters
import org.apache.mahout.common.iterator.sequencefile.PathType
import org.apache.mahout.common.iterator.sequencefile.SequenceFileDirIterable
import org.apache.mahout.math.DenseVector
import org.apache.mahout.math.Vector
import org.apache.mahout.math.VectorWritable
import net.duboue.thoughtland.CloudPoints
import net.duboue.thoughtland.Clusterer
import net.duboue.thoughtland.Component
import net.duboue.thoughtland.Components
import net.duboue.thoughtland.Environment
import org.apache.mahout.common.distance.TanimotoDistanceMeasure
import org.apache.mahout.common.distance.CosineDistanceMeasure
import org.apache.mahout.common.distance.ManhattanDistanceMeasure
import org.apache.mahout.common.distance.EuclideanDistanceMeasure
import org.apache.mahout.common.RandomUtils

class MahoutClusterer extends Clusterer {
  def apply(cloud: CloudPoints, numIter: Int)(implicit env: Environment): Components = {
    RandomUtils.useTestSeed()

    // turn the numbers into vectors
    val vectors = cloud.points.map { d => new DenseVector(d, true) }

    val conf = new Configuration()
    val fs = FileSystem.getLocal(conf)
    val valbaseDir = new Path(env.tmpDir.toURI())
    val seqFile = new Path(valbaseDir, "input.seq")
    val stateDir = new Path(valbaseDir, "state")
    val outputDir0 = new Path(valbaseDir, "output0")
    val outputDir = new Path(valbaseDir, "output")

    // write them
    val writer = new SequenceFile.Writer(fs, conf, seqFile, classOf[Text], classOf[VectorWritable])
    var i = 0
    for (vector <- vectors) {
      writer.append(new Text(s"point-$i"), new VectorWritable(vector));
      i += 1
    }
    writer.close();
//    System.out.println("Wrote")

    def cluster(output: Path, iter: Int, numCluster: Int) =
      DirichletDriver.run(conf, seqFile, output,
        new DistributionDescription(classOf[GaussianClusterDistribution].getName(), classOf[DenseVector].getName(),
          //            classOf[ManhattanDistanceMeasure].getName(),
          //            classOf[CosineDistanceMeasure].getName(),
          //            classOf[TanimotoDistanceMeasure].getName(),
          classOf[EuclideanDistanceMeasure].getName(),
          cloud.points(0).length),
        numCluster, iter, 1.0, true, false, 0.0001, true)

    val mainIter = numIter / 10

    cluster(outputDir0, mainIter, 1)
//    System.out.println("Main")
    cluster(outputDir, numIter, 20)
//    System.out.println("Cluster")

    def clusterToComponent(gaussian: GaussianCluster): Component = {
      implicit def vectorElemToDouble(v: Vector): Array[Double] =
        v.map { e => e.get() }.toArray
      Component(gaussian.getCenter(), gaussian.getRadius(), gaussian.getNumObservations())
    }

    val mainComponent = clusterToComponent(new SequenceFileDirIterable[IntWritable, ClusterWritable](new Path(outputDir0,
      s"clusters-$mainIter-final"), PathType.LIST, PathFilters.logsCRCFilter(),
      conf).head.getSecond().getValue().asInstanceOf[GaussianCluster])

    val parts = new SequenceFileDirIterable[IntWritable, ClusterWritable](new Path(outputDir,
      s"clusters-$numIter-final"), PathType.LIST, PathFilters.logsCRCFilter(), conf).map {
      record =>
        record.getSecond().getValue().asInstanceOf[GaussianCluster]
    } filter { gaussian => gaussian.getNumObservations() > 0 } map clusterToComponent

//    System.out.println(parts.size)
//    System.out.println(parts)
//    for (component <- parts)
//      System.out.println(component.center.mkString("", ", ", ""))

    Components(mainComponent, parts.toList)
  }
}

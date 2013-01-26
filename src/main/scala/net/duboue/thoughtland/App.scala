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
import org.apache.mahout.clustering.dirichlet.models.GaussianClusterDistribution
import org.apache.mahout.clustering.iterator.ClusterWritable
import org.apache.mahout.common.distance.EuclideanDistanceMeasure
import org.apache.mahout.common.iterator.sequencefile.PathFilters
import org.apache.mahout.common.iterator.sequencefile.PathType
import org.apache.mahout.common.iterator.sequencefile.SequenceFileDirIterable
import org.apache.mahout.math.Vector
import org.apache.mahout.math.DenseVector
import org.apache.mahout.math.VectorWritable

import com.google.common.io.Files

/**
 * @author Pablo Duboue <pablo.duboue@gmail.com>
 */
object App {

  def logGamma(x: Double): Double = {
    val tmp = (x - 0.5) * Math.log(x + 4.5) - (x + 4.5);
    val ser = 1.0 + 76.18009173 / (x + 0) - 86.50532033 / (x + 1)
    +24.01409822 / (x + 2) - 1.231739516 / (x + 3)
    +0.00120858003 / (x + 4) - 0.00000536382 / (x + 5);
    return tmp + Math.log(ser * Math.sqrt(2 * Math.PI));
  }

  def gamma(x: Double): Double = Math.exp(logGamma(x))

  def main(args: Array[String]) {

    // turn the numbers into vectors
    System.out.println("Starting with " + args(0))
    val doubles = Files.readLines(new File(args(0)), java.nio.charset.Charset.forName("utf-8")).
      map { x => x.split(",").map { _.toDouble } }
    System.out.println("Read")
    val vectors = doubles.map { d => new DenseVector(d, true) }
    //      val vector = new DenseVector(d.length)
    //      var i = 0
    //      while (i < d.length) {
    //        vector.setQuick(i, d(i))
    //        i += 1
    //      }
    //      vector
    //    }
    System.out.println("Vectorize")

    val conf = new Configuration()
    val fs = FileSystem.getLocal(conf)
    val valbaseDir = new Path("file:/tmp/thoughtland/")
    val seqFile = new Path(valbaseDir, "input.seq")
    val stateDir = new Path(valbaseDir, "state")
    val outputDir0 = new Path(valbaseDir, "output0")
    val outputDir = new Path(valbaseDir, "output")

    // write them
    val writer = new SequenceFile.Writer(fs, conf, seqFile, classOf[Text], classOf[VectorWritable]);
    var vectorWritable = new VectorWritable()
    var i = 0
    for (vector <- vectors) {
      vectorWritable.set(vector);
      writer.append(new Text("point-" + i), vectorWritable);
      i += 1
    }
    writer.close();
    System.out.println("Wrote")
    
    val numIter = 500

        DirichletDriver.run(conf, seqFile, outputDir0, new DistributionDescription(classOf[GaussianClusterDistribution].getName(), classOf[DenseVector].getName(), classOf[EuclideanDistanceMeasure].getName(),
          doubles(0).length), 1, 50, 1.0, false, true, 0.0, true)
        DirichletDriver.run(conf, seqFile, outputDir, new DistributionDescription(classOf[GaussianClusterDistribution].getName(), classOf[DenseVector].getName(), classOf[EuclideanDistanceMeasure].getName(),
          doubles(0).length), 10, numIter, 1.0, false, true, 0.2, true)

    System.out.println("Cluster")

    var mainRadius: Vector = null;
    var mainCenter: Vector = null;
    for (
      record <- new SequenceFileDirIterable[IntWritable, ClusterWritable](new Path(outputDir0, "clusters-50-final"), PathType.LIST, PathFilters.logsCRCFilter(), conf)
    ) {
      val gaussian = record.getSecond().getValue().asInstanceOf[GaussianCluster];
      mainRadius = gaussian.getRadius()
      mainCenter = gaussian.getCenter()
    }

    var centers = new scala.collection.mutable.ArrayBuffer[Vector]
    var radii = new scala.collection.mutable.ArrayBuffer[Vector]
    var covered = new scala.collection.mutable.ArrayBuffer[Long]

    for (
      record <- new SequenceFileDirIterable[IntWritable, ClusterWritable](new Path(outputDir, "clusters-"+numIter+"-final"), PathType.LIST, PathFilters.logsCRCFilter(), conf)
    ) {
      val gaussian = record.getSecond().getValue().asInstanceOf[GaussianCluster]
      if (gaussian.getNumObservations() > 0) {
        centers.add(gaussian.getCenter())
        radii.add(gaussian.getRadius())
        covered.add(gaussian.getNumObservations())
      }
    }

    System.out.println("Total number of components: " + centers.length)

    val nDiv2 = mainRadius.size() / 2.0
    val cn = Math.pow(Math.PI, nDiv2) / gamma(nDiv2 + 1)
    var fullVolume = cn
    for (n <- 0.to(mainRadius.size() - 1)) {
      fullVolume *= mainRadius.get(n)
    }
    val mainDensity = doubles.length / fullVolume

    for (i <- 0.to(centers.length - 1)) {
      System.out.print(i + " component is " + //
        (if (covered(i) > doubles.length / 2) "giant" else (if (covered(i) > doubles.length / 10) "big" else "small")) + //
        " (" + covered(i) + ")")
      var volume = cn
      for (n <- 0.to(mainRadius.size() - 1)) {
        volume *= radii(i).get(n)
      }
      val density = covered(i) / volume
      if (density > mainDensity)
        System.out.println(" and very dense")
      else if (density < mainDensity / 2)
        System.out.println(" and very sparse")
      else
        System.out.println()

      for (j <- (i + 1).to(centers.length - 1)) {
        System.out.print("  distance to " + j + " is ")
        var big = false
        var medium = false
        for (n <- 0.to(mainRadius.size() - 1)) {
          val delta = Math.abs(centers(i).get(n) - centers(j).get(n))
          val main = 3 * mainRadius.get(n) // * mainCenter.get(n))
          if (delta > main) {
            big = true
            //            System.out.println(delta + " " + mainRadius.get(n) + " " + main + " " + (main / 2))
          } else if (delta > main / 10)
            medium = true
        }
        if (big)
          System.out.println("big")
        else if (medium)
          System.out.println("medium")
        else
          System.out.println("small")
      }
    }

    /*
      // value is the cluster id as an int, key is the name/id of the
      // vector, but that doesn't matter because we only care about printing
      // it
      //String clusterId = value.toString();
      int keyValue = record.getFirst().get();
      List<WeightedVectorWritable> pointList = result.get(keyValue);
      if (pointList == null) {
        pointList = Lists.newArrayList();
        result.put(keyValue, pointList);
      }
      if (pointList.size() < maxPointsPerCluster) {
        pointList.add(record.getSecond());
      }
    }
    return result;
*      
     */

    //val dump = ClusterDumper.readPoints(new Path(outputDir, "clusters-50-final"), 100, conf);
    /*
    for (l <- dump.values) {
      for (v <- l) {
        System.out.print(v)
        System.out.print(' ')
      }
      System.out.println()
    }
  * 
  */
  }
}

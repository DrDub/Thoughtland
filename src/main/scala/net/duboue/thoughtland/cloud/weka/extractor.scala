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

import net.duboue.thoughtland.CloudExtractor
import net.duboue.thoughtland.TrainingData
import net.duboue.thoughtland.CloudPoints
import net.duboue.thoughtland.Environment
import weka.classifiers.Classifier
import java.lang.reflect.Field
import weka.core.converters.ConverterUtils.DataSource
import scala.collection.JavaConversions._
import weka.core.Instances
import scala.reflect.ClassTag
import scala.collection.Map
import scala.collection.mutable.HashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.io.File
import java.io.PrintWriter

abstract class WekaClassifierExtractor[T <: Classifier] {

  def pinpoint(clazz: Class[_], fieldName: String): Field = {
    val field = clazz.getDeclaredField(fieldName)
    field.setAccessible(true)
    field
  }

  def extractInt(obj: Any, field: Field) = field.getInt(obj)
  def extractDouble(obj: Any, field: Field) = field.getDouble(obj)
  def extractFloat(obj: Any, field: Field) = field.getFloat(obj)
  def extractBoolean(obj: Any, field: Field) = field.getBoolean(obj)

  def extract[X](obj: Any, field: Field): X = field.get(obj).asInstanceOf[X]

  def extract(classifier: T): Array[Double]
}

class WekaCloudExtractor extends CloudExtractor {
  def apply(data: TrainingData, algo: String, baseParams: Array[String])(implicit env: Environment): CloudPoints = {
    val params = baseParams ++ List("-S", env.config.randomSeed.toString)
    //Array[String]() //baseParams ++ List("-S" + env.config.randomSeed.toString)

    // get the data in RAM, data is assumed to be an ARFF file
    val instances = new DataSource(data.uri.toURL().toString().replace("file:/", "/")).getDataSet()
    //    val instances = new DataSource(data.uri.toString().replace("file:/", "file://")).getDataSet()
    val classIdx: Int = if (params.contains("-c")) {
      val idx = params.indexOf("-c")
      val r = params(idx + 1).toInt;
      params(idx) = "";
      params(idx + 1) = "";
      r
    } else instances.numAttributes() - 1
    instances.setClassIndex(classIdx)

    System.out.println(params.toList)

    // map the algo to the extractor
    val extractorName = "net.duboue.thoughtland.cloud.weka." + algo.split("\\.").last + "Extractor"
    val extractor = Class.forName(extractorName).newInstance().asInstanceOf[WekaClassifierExtractor[Classifier]]

    // do the leave-one-out in parallel
    case class WekaResults(points: Array[Double], expected: Double, returned: Double)

    val cpus = Runtime.getRuntime().availableProcessors()
    val threadPool = Executors.newFixedThreadPool(cpus); // this should use futures but I'm getting some maven errors with it

    val results = Array.ofDim[WekaResults](instances.numInstances())
    val leftOverTasks = new AtomicInteger(instances.numInstances());
    val lock = new Object
    var exc: Exception = null
    for (idx <- 0.to(instances.numInstances() - 1)) {
      threadPool.submit(new Runnable() {
        def run = {
          try {
            val foldInstances = new Instances(instances)
            val evalInstances = new Instances(instances)
            evalInstances.delete();
            evalInstances.add(foldInstances.instance(idx))
            val evalInstance = evalInstances.instance(0)
            foldInstances.delete(idx)
            val classifier = Class.forName(algo).newInstance().asInstanceOf[Classifier]
            classifier.setOptions(params.clone)
            classifier.buildClassifier(foldInstances)
            val classified = classifier.classifyInstance(evalInstance)
            results(idx) = WekaResults(extractor.extract(classifier), evalInstance.classValue(), classified)
            val left = leftOverTasks.decrementAndGet()
            System.out.println("Done " + idx + " left " + left)
            if (left == 0)
              lock.synchronized {
                lock.notifyAll()
              }
          } catch {
            case e: Exception => lock.synchronized {
              e.printStackTrace()
              exc = e
              lock.notifyAll()
            }
          }
        }
      })
    }
    lock.synchronized {
      lock.wait()
    }
    if (exc != null)
      throw exc

    // get the points
    var acc = 0.0
    val confusionMatrix = new HashMap[Double, Map[Double, Int]]

    val result: Seq[Array[Double]] = results map { r =>
      r match {
        case WekaResults(points, expected, actual) =>
          if (Math.abs(expected - actual) < expected * 0.1)
            acc += 1;
          if (!confusionMatrix.contains(expected))
            confusionMatrix += expected -> new HashMap[Double, Int]
          confusionMatrix(expected) += actual -> (confusionMatrix(expected).getOrElse(actual, 0) + 1)
          points
      }
    }
    acc /= instances.numInstances()
    System.out.println("Accuracy: " + acc)
    System.out.println("Confusion Matrix: " + confusionMatrix)

    if (env.config.storeResults) {
      val pwAcc = new PrintWriter(new File(env.resultsDir, "accuracy.txt"))
      val pwCM = new PrintWriter(new File(env.resultsDir, "confusion_matrix.tsv"))
      try {
        pwAcc.println(acc)
        for (p1: Pair[Double, Map[Double, Int]] <- confusionMatrix)
          for (p2: Pair[Double, Int] <- p1._2)
            pwCM.println(s"$p1._1\t$p2._1\t$p2._2")
      } finally {
        pwAcc.close()
        pwCM.close()
      }
    }

    return CloudPoints(result.toArray(ClassTag(classOf[Array[Double]])));
  }
}
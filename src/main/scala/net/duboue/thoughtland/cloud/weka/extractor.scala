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
import scala.actors.Futures
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import weka.core.Instances
import scala.reflect.ClassTag
import scala.collection.Map
import scala.collection.mutable.HashMap

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

    // get the data in RAM, data is assumed to be an ARFF file
    val instances = new DataSource(data.uri.toURL().toString()).getDataSet()
    //    val instances = new DataSource(data.uri.toString().replace("file:/", "file://")).getDataSet()

    // map the algo to the extractor
    System.out.println(algo)
    val extractorName = "net.duboue.thoughtland.cloud.weka." + algo.split("\\.").last + "Extractor"
    val extractor = Class.forName(extractorName).newInstance().asInstanceOf[WekaClassifierExtractor[Classifier]]

    // do the leave-one-out in parallel
    case class WekaResults(points: Array[Double], expected: Double, returned: Double)

    val results = Futures.awaitAll(0,
      0.to(instances.numInstances() - 1).map {
        idx =>
          Futures.future({
            val foldInstances = new Instances(instances)
            val evalInstances = new Instances(instances)
            evalInstances.delete();
            evalInstances.add(foldInstances.instance(idx))
            val evalInstance = evalInstances.instance(0)
            foldInstances.delete(idx)
            val classifier = Class.forName(algo).newInstance().asInstanceOf[Classifier]
            classifier.setOptions(params)
            classifier.buildClassifier(foldInstances)
            val classified = classifier.classifyInstance(evalInstance)
            WekaResults(extractor.extract(classifier), evalInstance.classValue(), classified)
          })
      }: _*)

    // get the points
    var acc = 0
    val confusionMatrix = new HashMap[Double, Map[Double, Int]]

    val result: Seq[Array[Double]] = results map { o => o.get } map { r =>
      r match {
        case WekaResults(points, expected, actual) =>
          if (expected == actual)
            acc += 1;
          if (!confusionMatrix.contains(expected))
            confusionMatrix += expected -> new HashMap[Double, Int]
          confusionMatrix(expected) += actual -> (confusionMatrix(expected).getOrElse(actual, 0) + 1)
          points
      }
    }
    return CloudPoints(result.toArray(ClassTag(classOf[Double])));
  }
}
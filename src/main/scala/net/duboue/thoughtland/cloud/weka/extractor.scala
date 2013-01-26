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
import scala.concurrent._
import scala.collection.JavaConversions._
import ExecutionContext.Implicits.global
import weka.core.Instances

abstract class WekaClassifierExtractor[T <: Classifier] {

  def pinpoint(clazz: Class[_], fieldName: String): Field = clazz.getField(fieldName)

  def extractInt(obj: Any, field: Field) = field.getInt(obj)
  def extractDouble(obj: Any, field: Field) = field.getDouble(obj)
  def extractFloat(obj: Any, field: Field) = field.getFloat(obj)
  def extractBoolean(obj: Any, field: Field) = field.getBoolean(obj)

  def extract[X](obj: Any, field: Field): X = field.get(obj).asInstanceOf[X]

  def extract(classifier: T): Array[Double]
}

class WekaCloudExtractor extends CloudExtractor {
  def apply(data: TrainingData, algo: String, params: Array[String])(implicit env: Environment): CloudPoints = {
    // get the data in RAM, data is assumed to be an ARFF file
    val instances = new DataSource(data.uri.toString()).getDataSet()

    // map the algo to the extractor
    val extractor = Class.forName("net.duboue.thoughtland.cloud.weka." + algo.split(".").last +
      "Extractor").newInstance().asInstanceOf[WekaClassifierExtractor[Classifier]]

    // do the leave-one-out in parallel
    case class WekaResults(points: Array[Double], expected: Double, returned: Double)

    val results = blocking {
      0.to(instances.numInstances() - 1).map {
        idx =>
          future[WekaResults] {
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
          }
      }
    }

    // get the points
    var acc = 0

    val result = results map { f =>
      f() match {
        case WekaResults(points, expected, actual) =>
          if (expected == actual)
            acc += 1;
          return points
      }
    }
    return null;
  }
}
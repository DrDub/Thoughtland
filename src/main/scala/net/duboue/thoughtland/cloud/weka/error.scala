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

import net.duboue.thoughtland.CloudPoints
import net.duboue.thoughtland.Environment
import net.duboue.thoughtland.TrainingData
import weka.classifiers.Classifier
import weka.core.Capabilities.Capability

/**
 * Extract an n-dimensional cloud of points where each point are the input attributes plus the error of a
 * cross-validated model on that input.
 */

class WekaErrorCloudExtractor extends WekaCrossValExtractor {
  def apply(data: TrainingData, algo: String, baseParams: Array[String])(implicit env: Environment): CloudPoints = {

    return apply(data, algo, baseParams,
      { (classifier, testInstance, expected, actual) =>
        val hasNumericClass = classifier.getCapabilities().handles(Capability.NUMERIC_CLASS)
        val array = testInstance.toDoubleArray()
        var i = 0
        while(i<array.length){
          if(array(i).isNaN())
            array(i) = 0
            i += 1
        }
        val error = if (hasNumericClass) Math.abs(expected - actual) else if (expected == actual) 0.0 else 1.0
        array(testInstance.classIndex()) = error * error // * 1000 //00
        array
      })
  }
}
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

package net.duboue.thoughtland.cloud.file

import net.duboue.thoughtland.CloudExtractor
import net.duboue.thoughtland.TrainingData
import net.duboue.thoughtland.CloudPoints
import net.duboue.thoughtland.Environment
import java.io.File
import scala.collection.JavaConversions._
import com.google.common.io.Files
import java.nio.charset.Charset

class FileCloudExtractor extends CloudExtractor {
  def apply(data: TrainingData, algo: String, baseParams: Array[String])(implicit env: Environment): CloudPoints = {
    val lines = Files.readLines(new File(data.uri.getPath()), Charset.forName("UTF-8"))
    val vectors = lines.map {
      line => line.split(",").map { s => s.toDouble }.toArray[Double]
    }.toArray[Array[Double]]
    CloudPoints(vectors)
  }
}
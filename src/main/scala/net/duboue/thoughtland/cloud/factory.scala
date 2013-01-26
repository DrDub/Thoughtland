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

package net.duboue.thoughtland.cloud

import net.duboue.thoughtland.CloudExtractor
import net.duboue.thoughtland.cloud.weka.WekaCloudExtractor
import net.duboue.thoughtland.cloud.file.FileCloudExtractor

sealed abstract class CloudExtractorEngine {
  def apply(): CloudExtractor
}

case class WekaEngine extends CloudExtractorEngine {
  def apply() = new WekaCloudExtractor()
}

case class FileEngine extends CloudExtractorEngine {
  def apply() = new FileCloudExtractor()
}

object CloudExtractorFactory {
  def apply(engine: CloudExtractorEngine) = engine()

  def apply(engine: String): CloudExtractor =  engine.toLowerCase() match {
    case "weka" => WekaEngine().apply()
    case "file" => FileEngine().apply()
  } 
}
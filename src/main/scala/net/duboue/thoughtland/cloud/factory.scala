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
import net.duboue.thoughtland.cloud.file.FileCloudExtractor
import net.duboue.thoughtland.cloud.weka.WekaCloudExtractor
import net.duboue.thoughtland.cloud.weka.WekaErrorCloudExtractor

object CloudExtractorEngine extends Enumeration {
  type CloudExtractorEngine = CloudExtractorEngineVal

  case class CloudExtractorEngineVal(name: String, make: () => CloudExtractor) extends Val(name)

  val Weka = CloudExtractorEngineVal("weka", { () => new WekaCloudExtractor() })
  val WekaError = CloudExtractorEngineVal("wekaerror", { () => new WekaErrorCloudExtractor() })
  val File = CloudExtractorEngineVal("file", { () => new FileCloudExtractor() })

  implicit def valueToCloudExtractorEngine(v: Value): CloudExtractorEngineVal = v.asInstanceOf[CloudExtractorEngineVal]
}

object CloudExtractorFactory {
  def apply(engine: CloudExtractorEngine.CloudExtractorEngine) = engine.make()
  def apply(engine: String): CloudExtractor = CloudExtractorEngine.withName(engine.toLowerCase()).make()
}
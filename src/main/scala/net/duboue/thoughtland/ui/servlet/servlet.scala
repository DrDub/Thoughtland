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

package net.duboue.thoughtland.ui.servlet;

import javax.servlet.Servlet
import java.io.PrintWriter
import org.scalatra.ScalatraServlet
import javax.servlet.ServletConfig
import java.util.Properties
import java.io.FileReader
import java.io.File

object ServletState {
  val prop = new Properties
  val lock = new Object

  var maxSize: Int = 0

  def init(fileName: Option[String]) {
    lock.synchronized {
      if (fileName.nonEmpty) {
        prop.load(new FileReader(fileName.get));
      } else {
        prop.setProperty("admin", "unknown (admin not set)");
        prop.setProperty("maxSizeStr", "500k");
        prop.setProperty("maxSizeBytes", "512000");
        prop.setProperty("dbDir", "/tmp");
      }
      dbDir = new File(prop.getProperty("dbDir"))
      maxSize = Integer.parseInt(prop.getProperty("maxSizeBytes"))
      load()
    }
  }

  private var dbDir: File = null
  
  private val runs: scala.collection.mutable.Buffer[Run] = new scala.collection.mutable.ArrayBuffer[Run]
  
  private def load() = lock.synchronized {
	  //TODO
  }

  object RunStatus extends Enumeration {
    type RunStatus = Value
    val RunError = Value("Error")
    val RunOngoing = Value("Ongoing")
    val RunFinished = Value("Finished")
  }

  case class Run(id: Int, data: File, status: RunStatus.RunStatus) {
    override def toString = s"Run $id ($status)" 
  }

  def runIds(): Array[Int] = runs.map { _.id }.toArray

  def runDescription(id: Int) = runs.find { _.id == id }.map { _.toString }.getOrElse("Unknown")
}

class ThoughtlandServlet extends ScalatraServlet {

  get("/") {
    <h1>Hello World!</h1>
  }
}


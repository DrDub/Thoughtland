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

object ServletState {
  val prop = new Properties

  def init(fileName: Option[String]) {
    if (fileName.nonEmpty) {
      prop.load(new FileReader(fileName.get));
    } else {
      prop.setProperty("admin", "non-set");
    }
  }

  def runIds(): Array[Int] = (1 :: List()).toArray // new Array[Int](0)
  
  def runDescription(id: Int) = "Unknown"

}

class ThoughtlandServlet extends ScalatraServlet {

  get("/") {
    <h1>Hello World!</h1>
  }
}


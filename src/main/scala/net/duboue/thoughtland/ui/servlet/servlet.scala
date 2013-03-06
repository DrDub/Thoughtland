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

import java.io.File
import java.io.FileReader
import java.nio.charset.Charset
import java.util.Properties

import scala.collection.JavaConversions._

import org.scalatra.ScalatraServlet

class ThoughtlandServlet extends ScalatraServlet {

  get("/") {
    <h1>Hello World!</h1>
  }
  
  post("/submission/new") {
    System.err.println("hola")
    System.out.println(request.body)
    System.err.println("hola");
    <h1>Got it!</h1>
    //TODO get the POST file
    //TODO get the owner, comments, extra from GET
    //TODO ServletState.enqueueRun(...)
  }
}


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

import org.scalatra.ScalatraServlet
import com.google.common.io.Files
import scala.collection.JavaConversions._
import com.google.common.io.CharStreams
import java.io.InputStreamReader
import javax.servlet.annotation.MultipartConfig
import org.scalatra.servlet.FileUploadSupport
import org.scalatra.servlet.SizeConstraintExceededException
import java.io.IOException
import java.io.File
import net.duboue.thoughtland.ui.servlet.ServletState.RunStatus

class ThoughtlandServlet extends ScalatraServlet with FileUploadSupport {
  error {
    case e: SizeConstraintExceededException => s"File upload exceeded ${ServletState.prop.getProperty("maxSizeStr")}."
    case e: IOException => s"Server error: $e."
  }

  get("/") {
    <h1>Hello World!</h1>
  }

  get("/submission/:id") {

    val id = params("id").toInt
    val status = ServletState.runStatus(id)
    val t = "function(){ document.getElementById('last_paragraph').scrollIntoView() }";
    <html>
      <head>
        {
          if (status == RunStatus.RunOngoing)
            <meta http-equiv="refresh" content="2"></meta>
        }
      </head>
      <body>
        <h1> Run { id } </h1>
        <p> Status: { status }  </p>
        <p> Log: </p>
        <ol>
          {
            for (line <- ServletState.runLog(id))
              yield (if (!line.trim.isEmpty) <li><tt> { line } </tt></li> else <br/>)
          }
        </ol>
        <p id="last_paragraph">
          <a name="last"> Status: { status }  </a>
        </p>
        <script type="text/javascript">
          window.setTimeout({ t } , 100);
        </script>
      </body>
    </html>
  }

  post("/submission/new") {
    val algo = request.getParameter("algo")

    if (ServletState.isLocked && !ServletState.lockedAlgos.contains(algo)) {
      <h1>Machine learning algorithm { algo } not accepted on a public server</h1>
    } else {
      def filterParams(list: List[String]): List[String] = list match {
        case List() => List()
        case l :: List() => List()
        case l1 :: l2 :: ls => if (ServletState.lockedParams.contains(l1))
          l1 :: l2.replaceAll("[^A-Za-z0-9.]", "") :: filterParams(ls)
        else
          filterParams(ls)
      }

      val params = filterParams(request.getParameter("params").split("\\s").toList)
      val owner = request.getParameter("name")
      val extra = request.getParameter("extra")
      val _private = request.getParameter("private")
      if (!_private.isEmpty) {
        System.out.println(new java.util.Date() + " " + request.remoteAddress + " " + _private)
      }
      val item = fileParams("upload_file")
      val tmpFile = File.createTempFile("upload", ".arff")
      item.write(tmpFile)
      //    System.err.println(new String(item.get, item.charset.getOrElse("UTF-8")))
      val id = ServletState.enqueueRun(tmpFile, owner, extra, algo, params.toArray, 500)
      tmpFile.delete()

      <a href={ s"/tl/submission/$id" }>Created submission { id }</a>
    }
  }
}


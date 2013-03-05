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
import com.google.common.io.Files
import net.duboue.thoughtland.ThoughtlandDriver
import java.io.PrintWriter
import net.duboue.thoughtland.TrainingData
import java.io.FileWriter
import java.io.FileOutputStream
import net.duboue.thoughtland.Environment
import net.duboue.thoughtland.Config

object ServletState {
  val prop = new Properties
  val lock = new Object
  val utf8 = Charset.forName("UTF-8")

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

  private def indexFile: File = new File(dbDir, "thoughtland.idx")
  private def load() = lock.synchronized {
    val index = indexFile;
    if (index.exists()) {
      Files.readLines(index, utf8).foreach {
        runs += lineToRun(_)
      }
    }
  }

  def save() = lock synchronized {
    val pw = new PrintWriter(indexFile)
    runs foreach { run =>
      pw.println(run.toLine)
    }
    pw.close
  }

  object RunStatus extends Enumeration {
    type RunStatus = Value
    val RunQueued = Value("Queued")
    val RunError = Value("Error")
    val RunOngoing = Value("Ongoing")
    val RunFinished = Value("Finished")
  }

  import RunStatus._

  case class Run(id: Int, prefix: String, status: RunStatus) {
    override def toString = s"Run $id ($status)"

    private def logFile = new File(dbDir, s"$prefix.log")
    def log(logLine: String) = lock synchronized {
      val pw = new PrintWriter(new FileWriter(logFile, true))
      pw.print(new java.util.Date())
      pw.print('\t')
      pw.println(logLine)
      pw.close
    }

    def log: List[String] = lock synchronized {
      Files.readLines(logFile, utf8).toList
    }

    private var _owner: String = null;
    private var _comment: String = null;
    private var _algo: String = null;
    private var _params: Array[String] = null;
    private var _numIter: Int = 0;

    def start(owner: String, comment: String, algo: String, params: Array[String], numIter: Int) {
      _owner = owner;
      _comment = comment;
      _algo = algo;
      _params = params;
      _numIter = numIter;
      val prop = new Properties
      prop.setProperty("owner", owner)
      prop.setProperty("comment", comment.replaceAll("\n", "\\n"))
      prop.setProperty("algo", algo)
      prop.setProperty("params", params.map { _.replaceAll("\\t", " ") }.mkString("\\t"))
      prop.setProperty("num_iter", s"$numIter")
      val f = new FileOutputStream(new File(s"$prefix.properties"))
      prop.save(f, toString)
      f.close
      log("Created")
    }

    private def loadProperties = lock synchronized {
      val prop = new Properties
      val reader = new FileReader(new File(s"$prefix.properties"))
      prop.load(reader)
      reader.close
      _owner = prop.getProperty("owner")
      _comment = prop.getProperty("comment").replaceAll("\\n", "\n")
      _algo = prop.getProperty("algo")
      _params = prop.getProperty("params").split("\\t")
      _numIter = Integer.parseInt(prop.getProperty("num_iter"))
    }

    private def retrieve[T](f: () => T) = lock synchronized {
      if (_algo == null)
        loadProperties
      f()
    }

    def owner = retrieve { () => _owner }
    def comment = retrieve { () => _comment }
    def algo = retrieve { () => _algo }
    def params = retrieve { () => _params }
    def numIter = retrieve { () => _numIter }

    def toLine(): String = s"$id\\t$prefix\\t$status"
  }

  private def lineToRun(l: String) = {
    val parts = l.split("\\t")
    Run(Integer.parseInt(parts(0)), parts(1), RunStatus.withName(parts(2)))
  }

  def runIds(): Array[Int] = lock synchronized { runs.map { _.id }.toArray }

  def runText(id: Int): String = lock synchronized {
    if (id < 0 || id >= runs.length) "" else {

      val run = runs(id)
      if (run.status != RunFinished)
        ""
      else
        Files.toString(new File(dbDir, s"${run.prefix}.txt"), utf8)
    }
  }

  def runStatus(id: Int): RunStatus = lock synchronized { runs(id).status }

  def runDescription(id: Int) = lock.synchronized { runs(id).toString }

  def runLog(id: Int) = lock synchronized {
    runs(id).log
  }

  def enqueueRun(arff: File, sentBy: String, comments: String, algo: String,
    params: Array[String], numIter: Int) = lock synchronized {
    val id = runs.length
    val prefix = s"run$id"
    val run = Run(id, prefix, RunQueued)
    runs += run
    Files.copy(arff, new File(dbDir, s"$prefix.arff"))
    run.start(sentBy, comments, algo, params, numIter)
    taskQueue.add(id)
  }

  private val taskQueue = new java.util.concurrent.LinkedBlockingQueue[Int](100)

  val executionThread = new Thread() {
    override def run() {
      val pipeline = ThoughtlandDriver("default");

      implicit val env = (Environment(dbDir, dbDir, Config(1, false)))
      while (true) {
        val id = taskQueue.take()
        var run: Run = null
        lock.synchronized {
          val previous = runs(id)
          run = Run(previous.id, previous.prefix, RunOngoing)
          runs(id) = run
          save()
        }
        //TODO this should be moved to a separate JVM so we can time-out and kill the process
        val generated = pipeline(TrainingData(new File(dbDir, s"${run.prefix}.arff").toURI),
          run.algo, run.params, run.numIter)
        val pw = new PrintWriter(new File(dbDir, s"${run.prefix}.txt"))
        pw.println(generated)
        pw.close
        lock.synchronized {
          val previous = runs(id)
          run = Run(previous.id, previous.prefix, RunFinished)
          runs(id) = run
          save()
        }
      }
    }
  }
}

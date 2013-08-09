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
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.OutputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.net.URI
import java.nio.charset.Charset
import java.util.Properties

import scala.Array.canBuildFrom
import scala.collection.JavaConversions.asScalaBuffer

import com.google.common.io.Files

import jlibs.core.lang.JavaProcessBuilder
import net.duboue.thoughtland.Config
import net.duboue.thoughtland.Environment
import net.duboue.thoughtland.ThoughtlandDriver
import net.duboue.thoughtland.TrainingData

object ServletState {
  val prop = new Properties
  val lock = new Object
  val utf8 = Charset.forName("UTF-8")

  var maxSize: Int = 0
  var locked: Boolean = true
  var funkyNames: Boolean = true
  val lockedAlgos = List("weka.classifiers.functions.MultilayerPerceptron", "weka.classifiers.functions.SMOreg").toArray
  val lockedParams = List("-H", "-c").toArray

  def init(fileName: Option[String]) {
    lock.synchronized {
      if (fileName.nonEmpty) {
        prop.load(new FileReader(fileName.get));
      } else {
        prop.setProperty("admin", "unknown (admin not set)");
        prop.setProperty("maxSizeStr", "5M");
        prop.setProperty("maxSizeBytes", "5242880");
        prop.setProperty("dbDir", "/tmp");
        prop.setProperty("locked", "false")
        prop.setProperty("funkyNames", "false")
      }
      dbDir = new File(prop.getProperty("dbDir"))
      maxSize = prop.getProperty("maxSizeBytes").toInt
      locked = prop.getProperty("locked").toBoolean
      funkyNames = prop.getProperty("funkyNames").toBoolean
      load()
      // re-start queued and interrupted
      for (run <- runs)
        if (run.status == RunStatus.RunQueued || run.status == RunStatus.RunOngoing)
          taskQueue.offer(run.id)
      executionThread.start()
    }
  }

  def getMaxSize = maxSize
  def isLocked = locked
  def useFunkyNames = funkyNames
  def getLockedAlgos = lockedAlgos
  def getLockedParams = lockedParams
  def getProperties = prop

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
      if (!logLine.trim.isEmpty) {
        pw.print(new java.util.Date())
        pw.print('\t')
        pw.println(logLine)
      }
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
      prop.setProperty("params", params.map { _.replaceAll("\t", " ") }.mkString("\t"))
      prop.setProperty("num_iter", s"$numIter")
      val f = new FileOutputStream(new File(dbDir, s"$prefix.properties"))
      prop.save(f, toString)
      f.close
      log("Created")
    }

    private def loadProperties = lock synchronized {
      val prop = new Properties
      val reader = new FileReader(new File(dbDir, s"$prefix.properties"))
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

    def toLine(): String = s"$id\t$prefix\t$status"
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
    save()
    taskQueue.put(id)
    id
  }

  private val taskQueue = new java.util.concurrent.LinkedBlockingQueue[Int](100)

  val executionThread = new Thread() {
    override def run() {
      while (true) {
        val id = taskQueue.take()
//        System.err.println(s"Got id=$id")
        var run: Run = null
        lock.synchronized {
          val previous = runs(id)
          run = Run(previous.id, previous.prefix, RunOngoing)
          runs(id) = run
          save()
        }
        val thisRun = run
        val tmpDir = new File(dbDir, s"run${run.id}.tmp")
        tmpDir.mkdir()
        val outFile = new File(dbDir, s"${run.prefix}.txt")
        val logStream = new PrintStream(new OutputStream() {
          val line = new StringBuilder
          def write(c: Int) = line.append(c.asInstanceOf[Char])
          override def flush = {
            run.log(line.toString)
            line.setLength(0)
          }
        }, true)

        val jvm = new JavaProcessBuilder();
//        System.out.println(System.getProperty("java.class.path"))
        jvm.classpath(System.getProperty("java.class.path"))
        jvm.maxHeap("6G")
        jvm.mainClass(PipelineApp.getClass.getName.replaceAll("\\$", "")) //classOf[PipelineApp].getName)// + "$")
        jvm.arg(new File(dbDir, s"${run.prefix}.arff").toURI.toString)
          .arg(dbDir.toString)
          .arg(tmpDir.toString)
          .arg(outFile.toString)
          .arg(run.algo)
          .arg(run.numIter.toString)
          .arg(useFunkyNames.toString)
        run.params.foreach { param => jvm.arg(param) }

        val process = jvm.launch(logStream, logStream)

        val threadToInterrupt = Thread.currentThread()
        val finished = new Array[Boolean](1)
        var result = 0

        if (ServletState.isLocked)
          // time-out and kill the process
          new Thread() {
            override def run: Unit = {
              Thread.sleep(1000 * 60 * 20)
              if (!finished(0)) {
                System.err.println("Run " + thisRun.id + " timed out")
                thisRun.log("Timed out")
                threadToInterrupt.interrupt()
              }
            }
          }.start();

        try {
          result = process.waitFor()
          finished(0) = true
        } catch {
          case e: InterruptedException =>
            process.destroy()
            result = -1
        }

        val success = result == 0 && outFile.exists

        lock.synchronized {
          val previous = runs(id)
          run = Run(previous.id, previous.prefix, if (success) RunFinished else RunError)
          runs(id) = run
          save()
        }
      }
    }
  }
}

object PipelineApp {

  def main(args: Array[String]) {
    val dataUri = new URI(args(0))
    val dbDir = new File(args(1))
    val tmpDir = new File(args(2))
    val outFile = new File(args(3))
    val algo = args(4)
    val numIter = args(5).toInt
    val useFunkyNames = args(6).toBoolean
    val params = args.drop(7)

    try {
      implicit val env = Environment(dbDir, tmpDir, Config(1, false, useFunkyNames))
      val generated = ThoughtlandDriver("default").apply(TrainingData(dataUri), algo,
        params, numIter)

      System.out.println(generated)

      val pw = new PrintWriter(outFile)
      pw.println(generated)
      pw.close()
    } finally {
      System.exit(0)
    }
  }
}
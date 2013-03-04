package net.duboue.thoughtland

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp.WebAppContext

import net.duboue.thoughtland.ui.servlet.ServletState
import net.duboue.thoughtland.ui.servlet.ThoughtlandServlet

object Main {

  def main(args: Array[String]): Unit = {
    
    ServletState.init(args.headOption)

    val server = new Server(7071); // TOTL
    val context = new WebAppContext()
    context.setContextPath("/")
    context.setResourceBase("src/main/webapp")
    context.addServlet(classOf[ThoughtlandServlet], "/tl/*")
    context.addServlet(classOf[DefaultServlet], "/")

    server.setHandler(context)
    server.start
    server.join
  }

}
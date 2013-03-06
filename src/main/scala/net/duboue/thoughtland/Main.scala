package net.duboue.thoughtland

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp.WebAppContext
import net.duboue.thoughtland.ui.servlet.ServletState
import net.duboue.thoughtland.ui.servlet.ThoughtlandServlet
import org.eclipse.jetty.servlet.ServletHolder
import javax.servlet.MultipartConfigElement

object Main {

  def main(args: Array[String]): Unit = {

    ServletState.init(args.headOption)

    val server = new Server(7071); // TOTL

    val configuration: Array[String] = List(
      "org.eclipse.jetty.webapp.WebInfConfiguration",
      "org.eclipse.jetty.webapp.WebXmlConfiguration",
      "org.eclipse.jetty.webapp.MetaInfConfiguration",
      "org.eclipse.jetty.webapp.FragmentConfiguration",
      "org.eclipse.jetty.plus.webapp.EnvConfiguration",
      "org.eclipse.jetty.plus.webapp.PlusConfiguration",
      "org.eclipse.jetty.annotations.AnnotationConfiguration",
      "org.eclipse.jetty.webapp.JettyWebXmlConfiguration").toArray;
    server.setAttribute("org.eclipse.jetty.webapp.configuration", configuration);

    val context = new WebAppContext()
    context.setContextPath("/")
    context.setResourceBase("src/main/webapp")
    val thoughtlandServlet = new ServletHolder(new ThoughtlandServlet());
    thoughtlandServlet.getRegistration().setMultipartConfig(new MultipartConfigElement("/tmp", ServletState.maxSize,
      ServletState.maxSize, 2 * ServletState.maxSize));
    context.addServlet(thoughtlandServlet, "/tl/*")
    context.addServlet(classOf[DefaultServlet], "/")

    server.setHandler(context)
    server.start
    server.join
  }

}
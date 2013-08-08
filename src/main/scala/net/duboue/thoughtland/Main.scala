package net.duboue.thoughtland

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp.WebAppContext
import net.duboue.thoughtland.ui.servlet.ServletState
import net.duboue.thoughtland.ui.servlet.ThoughtlandServlet
import org.eclipse.jetty.servlet.ServletHolder
import javax.servlet.MultipartConfigElement
import org.eclipse.jetty.server.ServerConnector

object Main {

  def main(args: Array[String]): Unit = {

    ServletState.init(args.headOption)

    val server = if (!ServletState.isLocked) new Server() else new Server(7071);
    if (!ServletState.isLocked) {
      val connector = new ServerConnector(server);
      connector.setHost("localhost");
      connector.setPort(7071); // TOTL in l33t
      server.addConnector(connector);
    }

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
    
    val webDir = Main.getClass().getClassLoader().getResource("net/duboue/thoughtland/webapp").toExternalForm();
    context.setResourceBase(webDir); //"src/main/resources/net/duboue/thoughtland/webapp")
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
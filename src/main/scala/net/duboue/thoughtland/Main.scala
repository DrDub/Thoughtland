package net.duboue.thoughtland

import org.mortbay.jetty.handler.ResourceHandler
import org.mortbay.jetty.Server
import org.mortbay.jetty.handler.HandlerList
import org.mortbay.jetty.handler.DefaultHandler
import org.mortbay.resource.Resource
import net.duboue.thoughtland.ui.servlet.ThoughtlandServlet
import org.mortbay.jetty.servlet.ServletHandler

object Main {

  def main(args: Array[String]): Unit = {
    val server = new Server(7071); // TOTL
    val resource_handler = new ResourceHandler();
    resource_handler.setWelcomeFiles(("index.html" :: List()).toArray);
    resource_handler.setBaseResource(Resource.newClassPathResource("/net/duboue/thoughtland/ui/servlet/static", true, false));
    val servletHandler = new ServletHandler();
    servletHandler.addServletWithMapping(classOf[ThoughtlandServlet], "/tl");
    val handlers = new HandlerList();
    handlers.setHandlers((servletHandler :: resource_handler :: new DefaultHandler() :: List()).toArray);
    server.setHandler(handlers);
    server.start();
    server.join();
  }

}
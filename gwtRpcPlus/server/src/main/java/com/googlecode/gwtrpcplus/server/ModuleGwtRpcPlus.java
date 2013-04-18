package com.googlecode.gwtrpcplus.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.servlet.ServletModule;
import com.googlecode.gwtrpcplus.server.servlet.GwtRpcPlusBasicServlet;
import com.googlecode.gwtrpcplus.server.servlet.GwtRpcPlusWebsocketDummy;
import com.googlecode.gwtrpcplus.server.util.Logger;


public class ModuleGwtRpcPlus extends ServletModule {
  private final static Logger logger = new Logger(ModuleGwtRpcPlus.class);

  private final String modulename;
  private final Set<Class<? extends RemoteServiceServlet>> servletClasses;


  /**
   * @param base for example the projectName
   * @param servletClasses Set of all ServletClasses
   */
  public ModuleGwtRpcPlus(String modulename,
      @SuppressWarnings("unchecked") Class<? extends RemoteServiceServlet>... servletClasses) {
    Set<Class<? extends RemoteServiceServlet>> classes = new HashSet<Class<? extends RemoteServiceServlet>>();
    for (Class<? extends RemoteServiceServlet> c : servletClasses)
      classes.add(c);

    this.modulename = modulename;
    this.servletClasses = classes;
  }

  /**
   * @param base for example the projectName
   * @param servletClasses Set of all ServletClasses
   */
  public ModuleGwtRpcPlus(String modulename, Set<Class<? extends RemoteServiceServlet>> servletClasses) {
    this.modulename = modulename;
    this.servletClasses = servletClasses;
  }

  @Override
  protected void configureServlets() {
    // WebsocketConnection
    boolean websocketsAdded = addWebsockets();

    // Serve with dummy, returning a NotImplemented-State
    if (!websocketsAdded) {
      serve("/" + modulename + "/gwtRpcPlusWebsocket").with(GwtRpcPlusWebsocketDummy.class);
    }

    // ConnectionBasic
    serve("/" + modulename + "/gwtRpcPlusBasic").with(GwtRpcPlusBasicServlet.class);

    bind(ServletList.class).toInstance(new ServletList() {
      @Override
      public Set<Class<? extends RemoteServiceServlet>> getServletClasses() {
        return servletClasses;
      }
    });
  }

  private boolean addWebsockets() {
    try {
      @SuppressWarnings("unchecked")
      Class<? extends HttpServlet> c = (Class<? extends HttpServlet>) Class.forName("com.googlecode.gwtrpcplus.server.servlet.GwtRpcPlusWebsocket");

      // Check for correct Jetty Version
      if (!getServletContext().getServerInfo().startsWith("jetty/9.")) {
        throw new RuntimeException("Only supported in jetty 9 yet (working since jetty 9.0.0), but was "
            + getServletContext().getServerInfo());
      }

      // Try adding the WebsocketServlet
      Map<String, String> params = new HashMap<String, String>();
      params.put("bufferSize", "100000");
      serve("/" + modulename + "/gwtRpcPlusWebsocket").with(c, params);
      return true;
    } catch (ClassNotFoundException e) {
      // Ignore when not added
      return false;
    } catch (Throwable e) {
      logger.trace("Ignoring creation the WebSocketServlet. Using only HTTP Calls. Exception", e);
      logger.warn("Ignoring creation the WebSocketServlet. Using only HTTP Calls. Exception:" + e.getClass().getName()
          + " :: " + e.getMessage());
      return false;
    }
  }
}

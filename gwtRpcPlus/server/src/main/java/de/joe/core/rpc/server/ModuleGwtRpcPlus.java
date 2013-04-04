package de.joe.core.rpc.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.servlet.ServletModule;

import de.joe.core.rpc.server.servlet.GwtRpcPlusBasicServlet;
import de.joe.core.rpc.server.servlet.GwtRpcPlusWebsocket;
import de.joe.core.rpc.server.servlet.GwtRpcPlusWebsocketDummy;
import de.joe.core.rpc.server.util.Logger;

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
    try {
      addWebsockets();
    } catch (Throwable e) {
      logger.trace("Ignoring creation the WebSocketServlet. Using only HTTP Calls. Exception", e);
      logger.warn("Ignoring creation the WebSocketServlet. Using only HTTP Calls. Exception:" + e.getClass().getName()
          + " :: " + e.getMessage());
      // Serve with dummy, returning a NotImplemented-State
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

  private void addWebsockets() throws Throwable {
    // Check for correct Jetty Version
    if (!getServletContext().getServerInfo().startsWith("jetty/9.")) {
      throw new RuntimeException("Only supported in jetty 9 yet (working since jetty 9.0.0.M3), but was "
          + getServletContext().getServerInfo());

    }

    // Try adding the WebsocketServlet
    Map<String, String> params = new HashMap<String, String>();
    params.put("bufferSize", "100000");
    serve("/" + modulename + "/gwtRpcPlusWebsocket").with(GwtRpcPlusWebsocket.class, params);
  }
}

package de.joe.core.rpc.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.servlet.ServletModule;

import de.joe.core.rpc.server.impl.GwtRpcPlusBasicServlet;
import de.joe.core.rpc.server.impl.GwtRpcPlusWebsocket;
import de.joe.core.rpc.server.impl.GwtRpcPlusWebsocketDummy;

public class ModulGwtRpcPlus extends ServletModule {
  private final static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ModulGwtRpcPlus.class);

  private final String modulename;
  private final Set<Class<? extends RemoteServiceServlet>> servletClasses;

  /**
   * @param base for example the projectName
   * @param servletClasses Set of all ServletClasses
   */
  public ModulGwtRpcPlus(String modulename, Set<Class<? extends RemoteServiceServlet>> servletClasses) {
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

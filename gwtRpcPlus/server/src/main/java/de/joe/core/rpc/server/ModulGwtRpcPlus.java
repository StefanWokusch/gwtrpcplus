package de.joe.core.rpc.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.servlet.ServletModule;

import de.joe.core.rpc.server.impl.GwtRpcPlusBasicServlet;
import de.joe.core.rpc.server.impl.GwtRpcPlusWebsocket;

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
    try {
      Map<String, String> params = new HashMap<String, String>();
      params.put("bufferSize", "100000");
      serve("/" + modulename + "/gwtRpcPlusWebsocket").with(GwtRpcPlusWebsocket.class, params);

      // ConnectionBasic
      serve("/" + modulename + "/gwtRpcPlusBasic").with(GwtRpcPlusBasicServlet.class);


    } catch (Throwable e) {
      logger.error("Ignoring creation the WebSocket-Server. Using HTTP Calls.", e);
    }
    bind(ServletList.class).toInstance(new ServletList() {
      @Override
      public Set<Class<? extends RemoteServiceServlet>> getServletClasses() {
        return servletClasses;
      }
    });
  }
}

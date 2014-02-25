package com.googlecode.gwtrpcplus.server;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.googlecode.gwtrpcplus.server.internal.SimpleGwtRpcPlusContext;
import com.googlecode.gwtrpcplus.server.internal.RpcManagerServer;
import com.googlecode.gwtrpcplus.server.internal.servlet.GwtRpcPlusBasicServlet;
import com.googlecode.gwtrpcplus.server.internal.servlet.GwtRpcPlusBundleServlet;
import com.googlecode.gwtrpcplus.server.internal.servlet.GwtRpcPlusWebsocketDummy;
import com.googlecode.gwtrpcplus.server.internal.type.RequestMethodHandlerBasic;
import com.googlecode.gwtrpcplus.server.internal.type.RequestMethodHandlerQueued;
import com.googlecode.gwtrpcplus.server.internal.type.RequestMethodHandlerServerpush;
import com.googlecode.gwtrpcplus.server.internal.util.Logger;
import com.googlecode.gwtrpcplus.server.internal.util.RpcHelper;
import com.googlecode.gwtrpcplus.server.internal.util.SimpleServletConfig;


public class GwtRpcPlusFilter implements Filter {
  private final static Logger logger = new Logger(GwtRpcPlusFilter.class);

  private static final String ATTRIBUTE_NAME = GwtRpcPlusContext.class.getName();

  private ServletContext servletContext;
  private GwtRpcPlusContext context;

  protected ServletContext getServletContext() {
    return servletContext;
  }

  protected GwtRpcPlusContext getContext(ServletContext servletContext) {
    if (context == null) {
      context = getGwtRpcPlusContext(servletContext);
    }
    return context;
  }

  public static GwtRpcPlusContext getGwtRpcPlusContext(ServletContext servletContext) {
    GwtRpcPlusContext instance = (GwtRpcPlusContext) servletContext.getAttribute(ATTRIBUTE_NAME);
    if (instance == null) {
      instance = new SimpleGwtRpcPlusContext();
      setGwtRpcPlusContext(servletContext, instance);
    }
    return instance;
  }

  public void setGwtRpcPlusContext(GwtRpcPlusContext context) {
    this.context = context;
  }

  /**
   * For default usage, use getGwtRpcPlusContext()
   * 
   * @param servletContext
   * @param instance
   */
  public static void setGwtRpcPlusContext(ServletContext servletContext, GwtRpcPlusContext instance) {
    if (servletContext.getAttribute(ATTRIBUTE_NAME) != null)
      throw new IllegalStateException("An other GwtRpcPlusContext has been initialized.");

    servletContext.setAttribute(ATTRIBUTE_NAME, instance);
  }

  private final HashMap<String, HttpServlet> servlets = new HashMap<>();

  protected RpcManagerServer manager;


  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    servletContext = filterConfig.getServletContext();

    GwtRpcPlusContext gwtRpcPlusContext = getContext(servletContext);
    RpcHelper helper = new RpcHelper(gwtRpcPlusContext);

    RequestMethodHandlerQueued queued = new RequestMethodHandlerQueued(helper);
    RequestMethodHandlerServerpush push = new RequestMethodHandlerServerpush(helper);
    RequestMethodHandlerBasic basic = new RequestMethodHandlerBasic(helper);
    manager = new RpcManagerServer(basic, push, queued);

    GwtRpcPlusBasicServlet basicServlet = new GwtRpcPlusBasicServlet(manager);
    registerServlet("gwtRpcPlusBasic", basicServlet);

    GwtRpcPlusBundleServlet bundleServlet = new GwtRpcPlusBundleServlet(manager);
    registerServlet("gwtRpcPlusBundle", bundleServlet);

    initWebsocket();

    for (RemoteServiceServlet servlet : gwtRpcPlusContext.getServlets())
      if (servlet.getServletConfig() == null)
        try {
          servlet.init(new SimpleServletConfig(servlet.getClass().getSimpleName(), servletContext));
        } catch (ServletException e) {
          logger.warn("Can't initialize Servlet. This can cause some Problems.", e);
        }

  }

  protected void initWebsocket() throws ServletException {
    GwtRpcPlusWebsocketDummy websocketDummy = new GwtRpcPlusWebsocketDummy();
    registerServlet("gwtRpcPlusWebsocket", websocketDummy);
  }

  /**
   * Adds a Servlet. It will call the init Method of the Servlet.
   * 
   * @param urlPattern pattern the servlet should be visible
   * @param servlet the Servlet to add
   * @throws ServletException
   */
  protected void registerServlet(String urlPattern, HttpServlet servlet) throws ServletException {
    servlets.put(urlPattern, servlet);

    servlet.init(new SimpleServletConfig("RpcPlus-" + urlPattern, servletContext));
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain filterChain)
      throws IOException, ServletException {
    final HttpServletRequest request = (HttpServletRequest) servletRequest;
    String path = request.getRequestURI().substring(request.getContextPath().length());

    if (!serve(servletRequest, response, path)) {
      // Not for JWT, so nothing to do for us
      filterChain.doFilter(servletRequest, response);
    }
  }

  protected boolean serve(ServletRequest servletRequest, ServletResponse response, String path)
      throws ServletException, IOException {
    // Remove starting slash
    if (path.length() <= 1)
      return false;
    path = path.substring(1);

    if(!path.contains("/"))
      return false;
    
    String servletUri = path.substring(path.lastIndexOf("/"));

    HttpServlet servlet = getServlet(servletUri);
    if (servlet != null) {
      // Request to JWT detected
      servlet.service(servletRequest, response);
      return true;
    }

    return false;
  }

  private HttpServlet getServlet(String servletUri) {
    return servlets.get(servletUri);
  }

  @Override
  public void destroy() {
  }

}

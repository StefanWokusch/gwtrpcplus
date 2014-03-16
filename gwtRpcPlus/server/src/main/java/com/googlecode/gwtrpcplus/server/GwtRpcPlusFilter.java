package com.googlecode.gwtrpcplus.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import com.googlecode.gwtrpcplus.server.internal.RpcManagerServer;
import com.googlecode.gwtrpcplus.server.internal.SimpleGwtRpcPlusContext;
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

  protected ServletContext getServletContext() {
    return servletContext;
  }

  private GwtRpcPlusContext context;

  /**
   * Only use this when you sure have a servletContext
   * 
   * @param servletContext must be not null
   * @return the gwtRpcPlusContext to register some Servlets
   */
  public GwtRpcPlusContext getContext(ServletContext servletContext) {
    if (context == null) {
      if (servletContext == null)
        throw new IllegalArgumentException("servletContext must not be null");
      context = getGwtRpcPlusContext(servletContext);
    }
    return context;
  }

  /**
   * Sets the Context for the Filter. This is only need in special environments, so make sure, you
   * need to do it.
   * 
   * @param context the context to set
   * @param servletContext optional ServletContext, to register the context
   */
  public void setContext(GwtRpcPlusContext context, ServletContext servletContext) {
    if (this.context != null)
      throw new IllegalStateException(
          "GwtRpcPlusContext is already set. This could caused for example using multiple GuiceModules.");
    this.context = context;

    // in some cases, we can have no servletContext, like guice in special environments
    if (servletContext != null)
      setGwtRpcPlusContext(context, servletContext);
  }

  /**
   * Only use this, when u have no other chance to get to the GwtRpcPlusContext
   * 
   * @param servletContext must be not null
   * @return the gwtRpcPlusContext to register Servlets
   */
  public static GwtRpcPlusContext getGwtRpcPlusContext(ServletContext servletContext) {
    if (servletContext == null)
      throw new IllegalArgumentException("servletContext must not be null");
    GwtRpcPlusContext instance = (GwtRpcPlusContext) servletContext.getAttribute(ATTRIBUTE_NAME);
    if (instance == null) {
      instance = new SimpleGwtRpcPlusContext();
      setGwtRpcPlusContext(instance, servletContext);
    }
    return instance;
  }


  private static void setGwtRpcPlusContext(GwtRpcPlusContext instance, ServletContext servletContext) {
    if (servletContext.getAttribute(ATTRIBUTE_NAME) != null)
      throw new IllegalStateException("An other GwtRpcPlusContext has been initialized.");
    servletContext.setAttribute(ATTRIBUTE_NAME, instance);
  }


  private String moduleName;

  /**
   * Sets the moduleName of the project to be used to filter only special calls
   * 
   * @param modulename
   */
  public void setModuleName(String modulename) {
    this.moduleName = modulename;
  }

  /**
   * @return the setted moduleName
   */
  public String getModuleName() {
    return moduleName;
  }

  private final HashMap<String, HttpServlet> servlets = new HashMap<>();

  protected RpcManagerServer manager;

  private ScheduledExecutorService executor;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    if (servletContext != null) {
      ServletContext c = filterConfig.getServletContext();
      // GwtRpcPlusFilter is already initialized. This should be no problem.
      // To be able to debug, a display warning, when its another servletContext.
      if (servletContext != c)
        logger.warn("GwtRpcPlusFilter is already initialized with another ServletContext. This can cause some Problems");
      return;
    }
    servletContext = filterConfig.getServletContext();

    executor = createExecutor();

    // Initialize
    GwtRpcPlusContext gwtRpcPlusContext = getContext(servletContext);
    RpcHelper helper = new RpcHelper(servletContext, gwtRpcPlusContext);
    RequestMethodHandlerQueued queued = new RequestMethodHandlerQueued(helper);
    RequestMethodHandlerServerpush push = new RequestMethodHandlerServerpush(helper);
    RequestMethodHandlerBasic basic = new RequestMethodHandlerBasic(helper);
    manager = new RpcManagerServer(basic, push, queued, executor);

    GwtRpcPlusBasicServlet basicServlet = new GwtRpcPlusBasicServlet(manager);
    registerServlet("gwtRpcPlusBasic", basicServlet);

    GwtRpcPlusBundleServlet bundleServlet = new GwtRpcPlusBundleServlet(manager);
    registerServlet("gwtRpcPlusBundle", bundleServlet);

    initWebsocket();

    helper.init();
  }

  protected ScheduledExecutorService createExecutor() {
    return Executors.newScheduledThreadPool(4);
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

    if (!path.contains("/"))
      return false;

    String[] split = path.split("/");

    String servletUri = split[split.length - 1];
    String moduleName = split[split.length - 2];

    if (this.moduleName != null && !this.moduleName.equals(moduleName))
      return false;

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
    try {
      executor.shutdownNow();
      executor.awaitTermination(10, TimeUnit.SECONDS);
    } catch (Exception e) {
      logger.error("Failed to shutdown ExecutorService.", e);
    }
  }
}

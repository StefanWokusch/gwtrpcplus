package com.googlecode.gwtrpcplus.server.internal.util;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.google.gwt.user.server.rpc.AbstractRemoteServiceServlet;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.googlecode.gwtrpcplus.server.GwtRpcPlusContext;

public class RpcHelper {
  private final static Logger logger = new Logger(RpcHelper.class);

  private final ServletContext servletContext;
  private final GwtRpcPlusContext context;

  public RpcHelper(ServletContext servletContext, GwtRpcPlusContext context) {
    this.context = context;
    this.servletContext = servletContext;
  }

  private ConcurrentHashMap<String, RemoteServiceServlet> servlets = new ConcurrentHashMap<>();

  public RemoteServiceServlet getServlet(String name) {
    RemoteServiceServlet result = servlets.get(name);
    if (result == null) {
      // Could be possible, that more servlets are added in the meantime
      // so init again and try it again
      init();
      result = servlets.get(name);
    }
    if (result != null)
      return result;
    logger.error("Servlet {} was not found in GwtRpcProcessor.", name);
    throw new IllegalArgumentException("Servlet \"" + name + "\" was not found in GwtRpcProcessor.");
  }

  public void init() {
    for (RemoteServiceServlet servlet : context.getServlets())
      if (servlet.getServletConfig() == null) {
        try {
          servlet.init(new SimpleServletConfig(servlet.getClass().getSimpleName(), servletContext));
          for (Class<?> iface : servlet.getClass().getInterfaces())
            servlets.put(iface.getSimpleName(), servlet);

        } catch (ServletException e) {
          logger.warn("Can't initialize Servlet. This can cause some Problems.", e);
        }
      }
  }

  @SuppressWarnings("unchecked")
  public void setThreadLocals(RemoteServiceServlet target, HttpServletRequest srcReq) {
    try {
      Field req = AbstractRemoteServiceServlet.class.getDeclaredField("perThreadRequest");
      // Give us access to hack them
      req.setAccessible(true);
      // Get the Attributes
      ThreadLocal<HttpServletRequest> targetReq = (ThreadLocal<HttpServletRequest>) req.get(target);
      // Default init
      if (targetReq == null) {
        synchronized (target) {
          targetReq = (ThreadLocal<HttpServletRequest>) req.get(target);
          if (targetReq == null)
            req.set(target, targetReq = new ThreadLocal<HttpServletRequest>());
        }
      }
      // Set the values
      targetReq.set(srcReq);
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }
}

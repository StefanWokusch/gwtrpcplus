package com.googlecode.gwtrpcplus.server.internal.util;

import java.lang.reflect.Field;

import javax.servlet.http.HttpServletRequest;

import com.google.gwt.user.server.rpc.AbstractRemoteServiceServlet;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.googlecode.gwtrpcplus.server.GwtRpcPlusContext;


public class RpcHelper {
  private final static Logger logger = new Logger(RpcHelper.class);

  private final GwtRpcPlusContext context;

  public RpcHelper(GwtRpcPlusContext context) {
    this.context = context;
  }

  public RemoteServiceServlet getServlet(String name) {
    for (RemoteServiceServlet servlet : context.getServlets())
      for (Class<?> iface : servlet.getClass().getInterfaces())
        if (iface.getSimpleName().equals(name))
          return servlet;
    logger.error("Servlet {} was not found in GwtRpcProcessor.", name);
    throw new IllegalArgumentException("Servlet \"" + name + "\" was not found in GwtRpcProcessor.");
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

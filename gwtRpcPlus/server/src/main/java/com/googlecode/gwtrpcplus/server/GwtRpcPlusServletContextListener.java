package com.googlecode.gwtrpcplus.server;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public abstract class GwtRpcPlusServletContextListener implements ServletContextListener {

  @Override
  public final void contextInitialized(ServletContextEvent sce) {
    contextInitialized(sce, GwtRpcPlusFilter.getGwtRpcPlusContext(sce.getServletContext()));
  }

  protected abstract void contextInitialized(ServletContextEvent sce, GwtRpcPlusContext gwtRpcPlusContext);
}

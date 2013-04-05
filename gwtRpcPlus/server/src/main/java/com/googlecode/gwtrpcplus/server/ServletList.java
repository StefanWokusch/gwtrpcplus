package com.googlecode.gwtrpcplus.server;

import java.util.Set;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * List of all registered RemoteServlets
 */
public interface ServletList {
  /**
   * @return all Servlets
   */
  Set<Class<? extends RemoteServiceServlet>> getServletClasses();
}

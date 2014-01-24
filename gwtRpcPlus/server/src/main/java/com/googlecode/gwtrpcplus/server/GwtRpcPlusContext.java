package com.googlecode.gwtrpcplus.server;

import java.util.Set;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public interface GwtRpcPlusContext {

  void register(RemoteServiceServlet servlet);

  void register(Class<? extends RemoteServiceServlet> servlet);

  Set<RemoteServiceServlet> getServlets();
}

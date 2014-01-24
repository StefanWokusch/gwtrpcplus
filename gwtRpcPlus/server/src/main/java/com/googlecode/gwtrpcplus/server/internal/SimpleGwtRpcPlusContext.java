package com.googlecode.gwtrpcplus.server.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.googlecode.gwtrpcplus.server.GwtRpcPlusContext;

public class SimpleGwtRpcPlusContext implements GwtRpcPlusContext {

  private final HashSet<RemoteServiceServlet> servlets = new HashSet<>();

  @Override
  public void register(Class<? extends RemoteServiceServlet> servlet) {
    try {
      register(servlet.newInstance());
    } catch (InstantiationException | IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void register(RemoteServiceServlet servlet) {
    servlets.add(servlet);
  }

  
  @Override
  public Set<RemoteServiceServlet> getServlets() {
    return Collections.unmodifiableSet(servlets);
  }
}

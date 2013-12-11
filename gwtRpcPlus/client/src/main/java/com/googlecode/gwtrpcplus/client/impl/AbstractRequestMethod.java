package com.googlecode.gwtrpcplus.client.impl;

import com.googlecode.gwtrpcplus.client.RequestMethod;
import com.googlecode.gwtrpcplus.client.RequestMethod.ConnectionHandler;

public abstract class AbstractRequestMethod implements RequestMethod, ConnectionHandler {

  private ConnectionHandler handler;

  @Override
  public void setHandler(ConnectionHandler handler) {
    this.handler = handler;
  }

  @Override
  public final void addRequest(RequestPlus request) {
    assertInit();
    handler.addRequest(request);
  }

  @Override
  public void removeRequest(RequestPlus request) {
    handler.removeRequest(request);
  }

  private void assertInit() {
    assert handler != null : "No Handler set";
  }

}

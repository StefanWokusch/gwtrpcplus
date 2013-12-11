package com.googlecode.gwtrpcplus.client.impl;

import com.googlecode.gwtrpcplus.client.Connection;
import com.googlecode.gwtrpcplus.client.Connection.RecieveHandler;

public abstract class AbstractConnection implements Connection, RecieveHandler {

  private RecieveHandler handler;

  @Override
  public void setHandler(RecieveHandler handler) {
    this.handler = handler;
  }

  private void assertInit() {
    assert handler != null : "No Handler set";
  }

  @Override
  public final void onRecieve(String answer) {
    assertInit();
    handler.onRecieve(answer);
  }

  @Override
  public final void onConnected() {
    assertInit();
    handler.onConnected();
  }

  @Override
  public final void onDisconnect() {
    assertInit();
    handler.onDisconnect();
  }

  @Override
  public final void onTimeout() {
    assertInit();
    handler.onTimeout();
  }

}

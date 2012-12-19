package de.joe.core.rpc.server;

import com.google.inject.Inject;

import de.joe.core.rpc.server.util.RpcHelper;

public class RequestMethodHandlerQueued extends RequestMethodHandlerBasic {

  @Override
  public String getRequestTypeName() {
    return "q";
  }

  @Inject
  public RequestMethodHandlerQueued(RpcHelper helper) {
    super(helper);
  }
}

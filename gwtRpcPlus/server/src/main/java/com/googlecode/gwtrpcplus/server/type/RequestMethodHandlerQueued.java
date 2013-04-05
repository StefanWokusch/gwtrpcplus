package com.googlecode.gwtrpcplus.server.type;

import com.google.inject.Inject;
import com.googlecode.gwtrpcplus.server.util.RpcHelper;


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

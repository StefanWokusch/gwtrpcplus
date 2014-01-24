package com.googlecode.gwtrpcplus.server.internal.type;

import com.googlecode.gwtrpcplus.server.internal.util.RpcHelper;


public class RequestMethodHandlerQueued extends RequestMethodHandlerBasic {

  @Override
  public String getRequestTypeName() {
    return "q";
  }

  public RequestMethodHandlerQueued(RpcHelper helper) {
    super(helper);
  }
}

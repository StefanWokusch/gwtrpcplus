package de.joe.core.rpc.client.impl;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;

import de.joe.core.rpc.client.util.RequestHelper;

public class RequestMethodBasic extends AbstractRequestMethod {
  // Visible for tests only
  final static String REQUEST_NAME = "b";

  private final String serviceName;


  public RequestMethodBasic(String serviceName) {
    this.serviceName = serviceName;
  }

  private final class BasicRequest implements RequestPlus {
    private final String requestData;
    private final RequestCallback callback;

    @Override
    public String getRequestTypeName() {
      return REQUEST_NAME;
    }

    private BasicRequest(String requestData, RequestCallback callback) {
      this.requestData = requestData;
      this.callback = callback;
    }

    @Override
    public String getRequestString() {
      return requestData;
    }

    @Override
    public String getServiceName() {
      return serviceName;
    }

    @Override
    public void onAnswer(String answer) {
      removeRequest(this);
      RequestHelper.process(callback, answer);
    }
  }

  @Override
  public Request call(String requestData, RequestCallback requestCallback) {
    addRequest(new BasicRequest(requestData, requestCallback));
    return null;
  }

  @Override
  public void onResendAll() {
    // TODO Auto-generated method stub
  }

}

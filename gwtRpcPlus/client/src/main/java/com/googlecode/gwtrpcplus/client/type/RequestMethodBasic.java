package com.googlecode.gwtrpcplus.client.type;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.googlecode.gwtrpcplus.client.impl.AbstractRequestMethod;
import com.googlecode.gwtrpcplus.client.util.RequestHelper;
import com.googlecode.gwtrpcplus.shared.InternalServerException;
import com.googlecode.gwtrpcplus.shared.TimeoutException;


public class RequestMethodBasic extends AbstractRequestMethod {
  // Visible for tests only
  public final static String REQUEST_NAME = "b";

  private final String serviceName;

  private final boolean resendAllowed;

  public RequestMethodBasic(String serviceName, boolean resendAllowed) {
    this.serviceName = serviceName;
    this.resendAllowed = resendAllowed;
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
      if (answer.startsWith("+")) {
        RequestHelper.process(callback, answer.substring(1));
      } else {
        callback.onError(null, new InternalServerException(answer.substring(1)));
      }
    }

    @Override
    public boolean onTimeout() {
      if (!resendAllowed) {
        removeRequest(this);
        callback.onError(null, new TimeoutException(resendAllowed));
      }
      return resendAllowed;
    }
  }

  @Override
  public Request call(String requestData, RequestCallback requestCallback) {
    addRequest(new BasicRequest(requestData, requestCallback));
    return null;
  }

}

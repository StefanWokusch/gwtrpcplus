package de.joe.core.rpc.client.impl;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.user.client.rpc.StatusCodeException;

import de.joe.core.rpc.client.util.RequestHelper;

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
        callback.onError(null, new StatusCodeException(500,// Internal ServerException
            "An Interal Serverexception was thrown. Look at the Serverlog for details"));
      }
    }

    @Override
    public boolean onTimeout() {
      if (!resendAllowed) {
        removeRequest(this);
        callback.onError(null, new StatusCodeException(408,// Request timeout
            "The Request timed out."));
      }
      return resendAllowed;
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

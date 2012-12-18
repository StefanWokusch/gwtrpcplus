package de.joe.core.rpc.client.impl;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;

import de.joe.core.rpc.client.util.RequestHelper;
import de.joe.core.rpc.client.util.UUID;
import de.joe.core.rpc.shared.InternalServerException;
import de.joe.core.rpc.shared.ServerPushCallback;
import de.joe.core.rpc.shared.TimeoutException;

public class RequestMethodServerpush extends AbstractRequestMethod {
  private final String serviceName;
  private final boolean resendAllowed;

  public RequestMethodServerpush(String serviceName, boolean resendAllowed) {
    this(serviceName, UUID.get(), resendAllowed);
  }

  private final UUID uuidfactory;


  public RequestMethodServerpush(String serviceName, UUID uuidfactory, boolean resendAllowed) {
    this.serviceName = serviceName;
    this.uuidfactory = uuidfactory;
    this.resendAllowed = resendAllowed;
  }

  private final class ServerpushRequest implements RequestPlus {
    private final String requestData;
    private final RequestCallback callback;

    @Override
    public String getRequestTypeName() {
      return "p";
    }

    private ServerpushRequest(String requestData, RequestCallback callback) {
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
    public void onAnswer(String response) {
      assert (response.startsWith("a") || response.startsWith("f") || response.startsWith("e")) : "Illegal ServerPush protocol";

      if (response.startsWith("e") || response.startsWith("f")) {
        removeRequest(this);
      }

      if (response.startsWith("e-")) {
        response = response.substring(2);
        callback.onError(null, new InternalServerException(response.substring(2)));
      } else {
        ServerPushCallback.nextIsFinished = response.startsWith("f");
        RequestHelper.process(callback, response.substring(1));
      }
    }

    @Override
    public boolean onTimeout() {
      callback.onError(null, new TimeoutException(resendAllowed));
      if (!resendAllowed)
        removeRequest(this);
      return resendAllowed;
    }
  }

  @Override
  public Request call(String requestData, RequestCallback requestCallback) {
    assert (requestCallback != null);
    final String uuid = uuidfactory.randomUUID();
    addRequest(new ServerpushRequest("s" + uuid + "#" + requestData, requestCallback));
    return new Request() {
      @Override
      public void cancel() {
        addRequest(new ServerpushRequest("c" + uuid, null));
      }

      @Override
      public boolean isPending() {
        assert (false) : "Not supported yet";
        return true;
      }
    };
  }

}

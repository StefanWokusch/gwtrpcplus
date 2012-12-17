package de.joe.core.rpc.client.impl;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;

import de.joe.core.rpc.client.util.RequestHelper;
import de.joe.core.rpc.client.util.UUID;

public class RequestMethodServerpush extends AbstractRequestMethod {

  private final String serviceName;

  public RequestMethodServerpush(String serviceName) {
    this(serviceName, UUID.get());
  }

  private final UUID uuidfactory;

  public RequestMethodServerpush(String serviceName, UUID uuidfactory) {
    this.serviceName = serviceName;
    this.uuidfactory = uuidfactory;
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

      String answer = response.substring(1);
      if (response.startsWith("e") || response.startsWith("f")) {
        removeRequest(this);
      }
      RequestHelper.process(callback, answer);
    }

    @Override
    public boolean onTimeout() {
      //FIXME timeout handling?
      return false;
    }
  }

  @Override
  public Request call(String requestData, RequestCallback requestCallback) {
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

  @Override
  public void onResendAll() {
    // TODO Auto-generated method stub
  }

}

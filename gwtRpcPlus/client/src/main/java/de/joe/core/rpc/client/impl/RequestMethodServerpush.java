package de.joe.core.rpc.client.impl;

import java.util.HashMap;

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

    private int expectedAnswer = 0;
    private final HashMap<Integer, String> queuedAnswers = new HashMap<Integer, String>();

    /**
     * ID of the next answer
     */
    @Override
    public void onAnswer(final String orgResponse) {
      assert (orgResponse.startsWith("a") || orgResponse.startsWith("f") || orgResponse.startsWith("e")) : "Illegal ServerPush protocol";

      if (orgResponse.startsWith("e-")) {
        callback.onError(null, new InternalServerException(orgResponse.substring(2)));
      } else {
        String response = orgResponse.substring(1);
        int markerIndex = response.indexOf("#");
        int answerId = Integer.parseInt(response.substring(0, markerIndex));
        String data = response.substring(markerIndex + 1);

        if (expectedAnswer != answerId) {
          // System.out.println("Unexpected Answer " + answerId + " (instead of " + expectedAnswer +
          // ")");
          queuedAnswers.put(answerId, orgResponse);
        } else {
          if (orgResponse.startsWith("e") || orgResponse.startsWith("f"))
            removeRequest(this);

          ServerPushCallback.nextIsFinished = orgResponse.startsWith("f");
          // System.out.println("Expected Answer " + expectedAnswer +
          // ServerPushCallback.nextIsFinished + " -> "
          // + orgResponse.substring(0, 10));
          RequestHelper.process(callback, data);
          expectedAnswer++;
          // Try adding the queued ones
          if (!queuedAnswers.isEmpty() && queuedAnswers.containsKey(expectedAnswer)) {
            // System.out.println("Late answer:: " + expectedAnswer);
            onAnswer(queuedAnswers.remove(expectedAnswer));
          }
        }
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

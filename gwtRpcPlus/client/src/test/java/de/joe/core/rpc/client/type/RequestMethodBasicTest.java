package de.joe.core.rpc.client.type;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;

import de.joe.core.rpc.client.RequestMethod;
import de.joe.core.rpc.client.RequestMethod.ConnectionHandler;
import de.joe.core.rpc.client.RequestMethod.RequestPlus;
import de.joe.core.rpc.client.type.RequestMethodBasic;

public class RequestMethodBasicTest {

  private RequestMethod method;

  private ArrayList<RequestPlus> allRequests;
  private ArrayList<RequestPlus> requests;

  private ConnectionHandler handler = new ConnectionHandler() {
    @Override
    public void addRequest(RequestPlus request) {
      allRequests.add(request);
      requests.add(request);
    }

    @Override
    public void removeRequest(RequestPlus request) {
      requests.remove(request);
    }
  };

  @Mock
  private RequestCallback callback;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
    allRequests = new ArrayList<>();
    requests = new ArrayList<>();
    method = new RequestMethodBasic("testService", true);
    method.setHandler(handler);
  }

  @Test
  public void testRequest() {


    method.call("reqData", callback);

    assertEquals(1, allRequests.size());
    assertEquals("testService", allRequests.get(0).getServiceName());
    assertEquals("reqData", allRequests.get(0).getRequestString());
  }

  @Test
  public void testAnswer() {
    method.call("reqData", callback);

    allRequests.get(0).onAnswer("+respData");

    verify(callback).onResponseReceived(any(Request.class), any(Response.class));
  }

  @Test
  public void testAnswerRemovesRequest() {
    method.call("reqData", callback);

    allRequests.get(0).onAnswer("respData");

    assertEquals(0, requests.size());
  }
}

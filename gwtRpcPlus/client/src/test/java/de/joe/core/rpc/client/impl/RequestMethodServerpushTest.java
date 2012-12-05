package de.joe.core.rpc.client.impl;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;

import de.joe.core.rpc.client.RequestMethod;
import de.joe.core.rpc.client.RequestMethod.ConnectionHandler;
import de.joe.core.rpc.client.RequestMethod.RequestPlus;
import de.joe.core.rpc.client.util.UUID;

public class RequestMethodServerpushTest {

  RequestMethod method;

  ArrayList<RequestPlus> allRequests;
  ArrayList<RequestPlus> requests;

  ConnectionHandler handler = new ConnectionHandler() {
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
  RequestCallback callback;

  Request lastRequest;
  Response lastResponse;
  List<String> allResponses;

  @Mock
  UUID uuid;

  private String id = "uuid1";

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
    allRequests = new ArrayList<>();
    requests = new ArrayList<>();
    allResponses = new ArrayList<>();
    lastRequest = null;
    lastResponse = null;
    when(uuid.randomUUID()).thenReturn(id);
    method = new RequestMethodServerpush("testService", uuid);
    method.setHandler(handler);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        lastRequest = (Request) invocation.getArguments()[0];
        lastResponse = (Response) invocation.getArguments()[1];
        if (lastRequest != null)
          allResponses.add(lastResponse.getText());
        return null;
      }
    }).when(callback).onResponseReceived(any(Request.class), any(Response.class));
  }

  private String createStartData() {
    return "s" + id + "#" + "reqData";
  }

  @Test
  public void testRequest() {


    method.call("reqData", callback);

    assertEquals(1, allRequests.size());
    assertEquals("testService", allRequests.get(0).getServiceName());
    assertEquals(createStartData(), allRequests.get(0).getRequestString());
  }


  @Test
  public void testSimpleAnswer() {
    method.call("reqData", callback);

    allRequests.get(0).onAnswer("arespData");

    assertEquals(createStartData(), allRequests.get(0).getRequestString());
    verify(callback).onResponseReceived(any(Request.class), any(Response.class));
    assertNotNull(lastResponse);
    assertEquals("respData", lastResponse.getText());
    assertEquals(1, requests.size());
  }

  @Test
  public void testFinishAnswer() {
    method.call("reqData", callback);

    allRequests.get(0).onAnswer("frespData");

    assertEquals(createStartData(), allRequests.get(0).getRequestString());
    verify(callback).onResponseReceived(any(Request.class), any(Response.class));
    assertNotNull(lastResponse);
    assertEquals("respData", lastResponse.getText());
    assertEquals(0, requests.size());
  }

  @Test
  public void testErrorAnswer() {
    method.call("reqData", callback);

    allRequests.get(0).onAnswer("erespData");

    assertEquals(createStartData(), allRequests.get(0).getRequestString());
    verify(callback).onResponseReceived(any(Request.class), any(Response.class));
    assertNotNull(lastResponse);
    assertEquals("respData", lastResponse.getText());
    assertEquals(0, requests.size());
  }

  @Test
  public void testMultipleAnswersPending() {
    method.call("reqData", callback);

    allRequests.get(0).onAnswer("a1");
    allRequests.get(0).onAnswer("a2");
    allRequests.get(0).onAnswer("a3");

    assertEquals(1, requests.size());
  }

  @Test
  public void testMultipleAnswersFinished() {
    method.call("reqData", callback);

    allRequests.get(0).onAnswer("a1");
    allRequests.get(0).onAnswer("a2");
    allRequests.get(0).onAnswer("f3");

    assertEquals(createStartData(), allRequests.get(0).getRequestString());
    assertNotNull(lastResponse);
    assertEquals(0, requests.size());
    assertEquals("1", allResponses.get(0));
    assertEquals("2", allResponses.get(1));
    assertEquals("3", allResponses.get(2));
  }
}

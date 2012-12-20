package de.joe.core.rpc.client.impl;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;

import de.joe.core.rpc.client.RequestMethod.ConnectionHandler;
import de.joe.core.rpc.client.RequestMethod.RequestPlus;
import de.joe.core.rpc.client.util.MyScheduler;
import de.joe.core.rpc.shared.PrioritisedRequest;

@SuppressWarnings("unused")
public class RequestMethodQueuedTest {

  private RequestMethodQueued method;

  private ArrayList<RequestPlus> allRequests;
  private ArrayList<RequestPlus> requests;

  private static class Scheduler implements MyScheduler {
    private Queue<ScheduledCommand> cmds = new ArrayBlockingQueue<>(10);

    public void doStuff() {
      while (cmds.size() > 0)
        cmds.poll().execute();
    }

    public void clear() {
      cmds.clear();
    }

    @Override
    public void scheduleFinaly(ScheduledCommand cmd) {
      cmds.add(cmd);
    }
  }

  private Scheduler scheduler = new Scheduler();

  private void doStuff() {
    scheduler.doStuff();
  }

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
    method = null;
  }

  private void init(int paralellrequests, boolean resend) {
    method = new RequestMethodQueued("testService", paralellrequests, resend);
    method.setHandler(handler);
    method.scheduler = scheduler;
  }

  @Test
  public void testSimpleRequest() {
    init(1, true);

    method.call("reqData", callback);
    doStuff();

    assertEquals(1, allRequests.size());
    assertEquals("testService", allRequests.get(0).getServiceName());
    assertEquals("reqData", allRequests.get(0).getRequestString());
  }

  @Test
  public void testTwoRequests_OneQueued() {
    init(1, true);

    method.call("reqData1", callback);
    method.call("reqData2", callback);
    doStuff();

    assertEquals(1, allRequests.size());
  }

  @Test
  public void testTwoRequests_OneAnswer_SecondSend() {
    init(1, true);

    method.call("reqData1", callback);
    method.call("reqData2", callback);
    doStuff();

    allRequests.get(0).onAnswer("+respData1");
    doStuff();

    verify(callback).onResponseReceived(any(Request.class), any(Response.class));
    assertEquals(2, allRequests.size());
  }

  @Test
  public void testTwoRequests_NoQueeud() {
    init(2, true);

    method.call("reqData1", callback);
    method.call("reqData2", callback);
    doStuff();

    assertEquals(2, allRequests.size());
  }

  @Test
  public void cancelPending_WaitsForAnswerBeforeSendNext() {
    init(1, true);
    Request r1 = method.call("reqData1", callback);
    Request r2 = method.call("reqData2", callback);
    doStuff();

    r1.cancel();
    doStuff();

    assertEquals(1, allRequests.size());
    doStuff();

    allRequests.get(0).onAnswer("+respData1");
    doStuff();

    assertEquals(2, allRequests.size());
  }

  @Test
  public void cancelPending_DontCallesCallback() {
    init(1, true);
    Request r1 = method.call("reqData1", callback);
    Request r2 = method.call("reqData2", callback);
    doStuff();

    r1.cancel();
    doStuff();

    allRequests.get(0).onAnswer("+respData1");
    doStuff();

    verify(callback, times(0)).onResponseReceived(any(Request.class), any(Response.class));
  }

  @Test
  public void prio_beforeSend_queue() {
    init(1, true);
    PrioritisedRequest r1 = (PrioritisedRequest) method.call("reqData1", callback);
    PrioritisedRequest r2 = (PrioritisedRequest) method.call("reqData2", callback);
    PrioritisedRequest r3 = (PrioritisedRequest) method.call("reqData3", callback);

    r1.setPriority(1);
    r2.setPriority(2);
    r2.setPriority(3);

    doStuff();

    assertEquals(1, allRequests.size());
    assertTrue(allRequests.get(0).getRequestString().endsWith("3"));
  }

  @Test
  public void prio_afterSend_queue() {
    init(1, true);
    PrioritisedRequest r1 = (PrioritisedRequest) method.call("reqData1", callback);
    PrioritisedRequest r2 = (PrioritisedRequest) method.call("reqData2", callback);
    PrioritisedRequest r3 = (PrioritisedRequest) method.call("reqData3", callback);

    doStuff();

    r1.setPriority(1);
    r2.setPriority(2);
    r2.setPriority(3);

    allRequests.get(0).onAnswer("+respData3");
    doStuff();

    assertEquals(2, allRequests.size());
    assertTrue(allRequests.get(0).getRequestString().endsWith("1"));
    assertTrue(allRequests.get(1).getRequestString().endsWith("3"));
  }
}

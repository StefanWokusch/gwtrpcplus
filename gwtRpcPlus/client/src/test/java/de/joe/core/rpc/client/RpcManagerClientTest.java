package de.joe.core.rpc.client;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.gwt.http.client.RequestCallback;

import de.joe.core.rpc.client.Connection;
import de.joe.core.rpc.client.Connection.RecieveHandler;
import de.joe.core.rpc.client.ConnectionProvider;
import de.joe.core.rpc.client.RequestMethod;
import de.joe.core.rpc.client.RequestMethod.ConnectionHandler;
import de.joe.core.rpc.client.RequestMethod.RequestPlus;
import de.joe.core.rpc.client.util.MyTimer;
import de.joe.core.rpc.client.RpcManagerClient;

public class RpcManagerClientTest {


  @Mock
  Connection connection;
  RecieveHandler connectionHandler;

  RpcManagerClient rpc;

  @Mock
  RpcManagerClient.RpcManagerHandler rpcHandler;

  @Mock
  RequestMethod method;
  ConnectionHandler methodHandler;

  @Mock
  RequestCallback gwtRpcCallback;
  @Mock
  MyTimer timer;

  String lastAnswer = null;

  private class TestRequest implements RequestPlus {
    private final boolean resend;

    public TestRequest(boolean resend) {
      this.resend = resend;
    }

    @Override
    public String getRequestTypeName() {
      return "t";
    }

    @Override
    public String getServiceName() {
      return "test";
    }

    @Override
    public String getRequestString() {
      return "data";
    }

    @Override
    public void onAnswer(String answer) {
      lastAnswer = answer;
    }

    @Override
    public boolean onTimeout() {
      return resend;
    }
  }

  // Initialization
  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);

    connectionHandler = null;
    lastAnswer = null;

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        methodHandler = (ConnectionHandler) invocation.getArguments()[0];
        return null;
      }
    }).when(method).setHandler(any(ConnectionHandler.class));

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        connectionHandler = (RecieveHandler) invocation.getArguments()[0];
        return null;
      }
    }).when(connection).setHandler(any(RecieveHandler.class));

    rpc = new RpcManagerClient(new ConnectionProvider() {
      @Override
      public List<Connection> get() {
        return new ArrayList<Connection>(Arrays.asList(new Connection[]{
          connection
        }));
      }
    });
    rpc.timer = timer;
    rpc.register(method);
    rpc.addHandler(rpcHandler);

    assertNotNull(connectionHandler);
    connectionHandler.onConnected();
  }

  @Test
  public void simpleAnswer() {
    TestRequest request = new TestRequest(true);
    methodHandler.addRequest(request);

    connectionHandler.onRecieve("1#answer");

    assertEquals("answer", lastAnswer);
  }


  @Test
  public void removeRequest() {
    TestRequest request = new TestRequest(true);
    methodHandler.addRequest(request);

    connectionHandler.onRecieve("1#answer");
    methodHandler.removeRequest(request);
    rpc.onTimeout();

    verify(connection, times(1)).send(anyString());
  }

  @Test
  public void setPending_true() {
    TestRequest request = new TestRequest(true);
    methodHandler.addRequest(request);

    verify(connection).setPending(true);
  }

  @Test
  public void setPending_false() {
    TestRequest request = new TestRequest(true);
    methodHandler.addRequest(request);
    verify(connection).setPending(true);

    methodHandler.removeRequest(request);

    verify(connection).setPending(false);
  }

  @Test
  public void noResponse_resendAllowed_resend() {
    TestRequest request = new TestRequest(true);
    methodHandler.addRequest(request);

    rpc.onTimeout();

    verify(connection, times(2)).send(anyString());
  }

  @Test
  public void noResponse_resendNotAllowed_noRresend() {
    TestRequest request = new TestRequest(false);
    methodHandler.addRequest(request);

    rpc.onTimeout();

    verify(connection, times(1)).send(anyString());
  }

  @Test
  public void timeoutHandler_withPending_fire() {
    TestRequest request = new TestRequest(true);
    methodHandler.addRequest(request);

    rpc.onTimeout();

    verify(rpcHandler, times(1)).onTimeout();
  }

  @Test
  public void timeoutHandler_withoutPending_dontfire() {
    TestRequest request = new TestRequest(true);
    methodHandler.addRequest(request);
    methodHandler.removeRequest(request);

    rpc.onTimeout();

    verify(rpcHandler, times(0)).onTimeout();
  }

}

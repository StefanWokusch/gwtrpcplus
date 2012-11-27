package de.joe.core.rpc.client.impl;

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
import de.joe.core.rpc.client.RpcManagerClient;

public class RpcManagerClientTest {

  @Mock
  Connection conHighPrio;
  RecieveHandler conHandlerHighPrio;

  @Mock
  Connection conMedPrio;
  RecieveHandler conHandlerMedPrio;

  @Mock
  Connection conLowPrio;
  RecieveHandler conHandlerLowPrio;

  RpcManagerClient rpc;
  RequestMethod method = new RequestMethodBasic("testService");

  @Mock
  RequestCallback requestCallback;

  // Initialization
  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);

    conHandlerHighPrio = null;
    conHandlerMedPrio = null;
    conHandlerLowPrio = null;

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        conHandlerHighPrio = (RecieveHandler) invocation.getArguments()[0];
        return null;
      }
    }).when(conHighPrio).setHandler(any(RecieveHandler.class));
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        conHandlerHighPrio.onDisconnect();
        return null;
      }
    }).when(conHighPrio).disconnect();

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        conHandlerMedPrio = (RecieveHandler) invocation.getArguments()[0];
        return null;
      }
    }).when(conMedPrio).setHandler(any(RecieveHandler.class));
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        conHandlerMedPrio.onDisconnect();
        return null;
      }
    }).when(conMedPrio).disconnect();

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        conHandlerLowPrio = (RecieveHandler) invocation.getArguments()[0];
        return null;
      }
    }).when(conLowPrio).setHandler(any(RecieveHandler.class));
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        conHandlerLowPrio.onDisconnect();
        return null;
      }
    }).when(conLowPrio).disconnect();

    rpc = new RpcManagerClient(new ConnectionProvider() {
      @Override
      public List<Connection> get() {
        return new ArrayList<Connection>(Arrays.asList(new Connection[]{
            conHighPrio, conMedPrio, conLowPrio
        }));
      }
    });
  }

  @Test
  public void checkHandlersSet() {
    assertNotNull(conHandlerLowPrio);
    assertNotNull(conHandlerMedPrio);
    assertNotNull(conHandlerHighPrio);
  }

  @Test
  public void connectAllOnStartup() {
    verify(conHighPrio).connect();
    verify(conMedPrio).connect();
    verify(conLowPrio).connect();
  }

  @Test
  public void disconnectLowerConnections_allAvailable() {
    conHandlerLowPrio.onConnected();
    conHandlerMedPrio.onConnected();
    conHandlerHighPrio.onConnected();

    verify(conLowPrio).disconnect();
    verify(conMedPrio).disconnect();
    verify(conHighPrio, never()).disconnect();
  }

  @Test
  public void disconnectMediumConnection_reconnectAll() {
    conHandlerLowPrio.onConnected();
    conHandlerMedPrio.onConnected();
    verify(conLowPrio).disconnect();

    conHandlerMedPrio.onDisconnect();

    verify(conLowPrio, times(2)).connect();
    verify(conMedPrio, times(2)).connect();
    verify(conHighPrio, times(1)).connect();
  }

  @Test
  public void makeCall_medConAvailable() {
    conHandlerMedPrio.onConnected();

    rpc.call(method, "reqData", requestCallback);

    verify(conMedPrio).send(eq("1#" + RequestMethodBasic.REQUEST_NAME + "#testService#reqData"));
  }
}

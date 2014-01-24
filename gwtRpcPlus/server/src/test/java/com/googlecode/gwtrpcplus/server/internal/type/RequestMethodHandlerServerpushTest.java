package com.googlecode.gwtrpcplus.server.internal.type;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.googlecode.gwtrpcplus.server.internal.type.RequestMethodHandler.RequestMethodAnswerer;
import com.googlecode.gwtrpcplus.server.internal.type.RequestMethodHandlerServerpush.RPCInterface;
import com.googlecode.gwtrpcplus.server.internal.util.RpcHelper;
import com.googlecode.gwtrpcplus.shared.CancelHandler;
import com.googlecode.gwtrpcplus.shared.ReturnHandler;


public class RequestMethodHandlerServerpushTest {


  @Mock
  RpcHelper helper;
  @Mock
  RPCInterface encoder;
  @Mock
  RequestMethodAnswerer answerer;

  private RequestMethodHandlerServerpush toTest;

  private TestServiceImpl service;

  @Before
  public void init() throws Throwable {
    MockitoAnnotations.initMocks(this);
    toTest = new RequestMethodHandlerServerpush(helper);
    toTest.encoder = encoder;
    service = new TestServiceImpl();

    final Method method = TestService.class.getMethod("testMethod", String.class, int.class);

    when(helper.getServlet(eq(SERVICENAME))).thenReturn(service);
    when(encoder.decodeRequest(anyString(), eq(TestServiceImpl.class), eq(service))).thenAnswer(
        new Answer<RPCRequest>() {
          @Override
          public RPCRequest answer(InvocationOnMock invocation) throws Throwable {
            String encodedRequest = (String) invocation.getArguments()[0];
            return new RPCRequest(method, new Object[]{
                encodedRequest, encodedRequest.length()
            }, null, 0);
          }
        });
    when(encoder.encodeResponseForSuccess(eq(method), any(), any(SerializationPolicy.class), anyInt())).thenAnswer(
        new Answer<String>() {
          @Override
          public String answer(InvocationOnMock invocation) throws Throwable {
            Object ret = invocation.getArguments()[1];
            return "|OK|" + ret + "|";
          }
        });
    when(encoder.encodeResponseForFailure(eq(method), any(Throwable.class), any(SerializationPolicy.class), anyInt())).thenAnswer(
        new Answer<String>() {
          @Override
          public String answer(InvocationOnMock invocation) throws Throwable {
            Object ret = invocation.getArguments()[1];
            return "|FAIL|" + ret + "|";
          }
        });
  }

  private static final String SERVICENAME = "TestService";

  public static interface TestService {
    String testMethod(String param1, int param2);
  }

  public class TestServiceImpl extends RemoteServiceServlet implements TestService {
    private static final long serialVersionUID = 1L;

    @Override
    public String testMethod(String param1, int param2) {
      assert (false) : "This should be never called";
      return null;
    }

    public String lastparam1;
    public int lastparam2;
    public ArrayList<ReturnHandler<String>> aliveHandlers = new ArrayList<>();

    public CancelHandler testMethod(String param1, int param2, final ReturnHandler<String> handler) {
      this.lastparam1 = param1;
      this.lastparam2 = param2;
      aliveHandlers.add(handler);
      return new CancelHandler() {
        @Override
        public void onCancel() {
          aliveHandlers.remove(handler);
        }
      };
    }
  }

  String testData = "data";

  String createStartData() {
    return "s" + "uuid1" + "#" + testData;
  }

  String createCancelData() {
    return "c" + "uuid1";
  }

  @Test
  public void simpleCalled() {
    toTest.process(SERVICENAME, createStartData(), null, answerer);

    assertNotNull(service.lastparam1);
    assertEquals(testData, service.lastparam1);
  }

  @Test
  public void simpleSended() {
    toTest.process(SERVICENAME, createStartData(), null, answerer);

    assertEquals(1, service.aliveHandlers.size());
    service.aliveHandlers.get(0).answer("a1");

    verify(answerer).send(eq("a0#|OK|a1|"));
  }

  @Test
  public void finish() {
    toTest.process(SERVICENAME, createStartData(), null, answerer);

    service.aliveHandlers.get(0).finish("a1");

    verify(answerer).send(eq("f0#|OK|a1|"));
  }

  @Test
  public void cancelRequest() {
    toTest.process(SERVICENAME, createStartData(), null, answerer);
    toTest.process(SERVICENAME, createCancelData(), null, answerer);

    assertEquals(0, service.aliveHandlers.size());
  }

}

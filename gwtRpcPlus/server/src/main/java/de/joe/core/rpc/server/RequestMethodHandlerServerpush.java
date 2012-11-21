package de.joe.core.rpc.server;

import java.lang.reflect.Method;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.inject.Inject;

import de.joe.core.rpc.server.util.RpcHelper;
import de.joe.core.rpc.shared.CancelHandler;
import de.joe.core.rpc.shared.ReturnHandler;

public class RequestMethodHandlerServerpush implements RequestMethodHandler {
  private final static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RequestMethodHandlerServerpush.class);

  private final RpcHelper helper;

  @Inject
  public RequestMethodHandlerServerpush(RpcHelper helper) {
    this.helper = helper;
  }


  interface RPCInterface {
    RPCRequest decodeRequest(String encodedRequest, Class<? extends RemoteServiceServlet> type,
        RemoteServiceServlet serializationPolicyProvider);

    String encodeResponseForSuccess(Method serviceMethod, Object object, SerializationPolicy serializationPolicy,
        int flags) throws SerializationException;

    String encodeResponseForFailure(Method serviceMethod, Throwable object, SerializationPolicy serializationPolicy,
        int flags) throws SerializationException;
  }

  // Visible for test only
  RPCInterface encoder = new RPCInterface() {
    @Override
    public RPCRequest decodeRequest(String encodedRequest, Class<? extends RemoteServiceServlet> type,
        RemoteServiceServlet serializationPolicyProvider) {
      return RPC.decodeRequest(encodedRequest, type, serializationPolicyProvider);
    }

    @Override
    public String encodeResponseForSuccess(Method serviceMethod, Object object,
        SerializationPolicy serializationPolicy, int flags) throws SerializationException {
      return RPC.encodeResponseForSuccess(serviceMethod, object, serializationPolicy, flags);
    }

    @Override
    public String encodeResponseForFailure(Method serviceMethod, Throwable object,
        SerializationPolicy serializationPolicy, int flags) throws SerializationException {
      return RPC.encodeResponseForFailure(serviceMethod, object, serializationPolicy, flags);
    }
  };


  private CancelHandler handler;

  @SuppressWarnings("rawtypes")
  @Override
  public void process(String service, String data, HttpServletRequest request, final RequestMethodAnswerer answerer) {
    if (data.startsWith("c")) {
      // TODO Cancel it
      handler.onCancel();
    } else if (data.startsWith("s")) {
      data = data.substring(1);
      RemoteServiceServlet servlet = helper.getServlet(service);
      helper.setThreadLocals(servlet, request);
      try {
        final RPCRequest rpcRequest;
        try {
          rpcRequest = encoder.decodeRequest(data, servlet.getClass(), servlet);
        } catch (NullPointerException e) {
          // This is only to avoid long exceptionstacks in logging
          // TODO fix this nullpointerbug instead
          logger.warn("Cant Process Request, because of not loaded Policies. This could caused by a Serverrestart.");
          return;
        }

        Class<?>[] callTypes = new Class<?>[rpcRequest.getMethod().getParameterTypes().length + 1];
        {
          int i = 0;
          for (Class<?> c : rpcRequest.getMethod().getParameterTypes())
            callTypes[i++] = c;
          callTypes[i] = ReturnHandler.class;
        }
        Method toCall = servlet.getClass().getMethod(rpcRequest.getMethod().getName(), callTypes);

        Object[] params = new Object[rpcRequest.getParameters().length + 1];
        {
          int i = 0;
          for (Object o : rpcRequest.getParameters())
            params[i++] = o;
          params[i] = new ReturnHandler() {
            @Override
            public void answer(Object obj) {
              try {
                String answer = encoder.encodeResponseForSuccess(rpcRequest.getMethod(), obj,
                    rpcRequest.getSerializationPolicy(), rpcRequest.getFlags());
                answerer.send("a" + answer);
              } catch (SerializationException e) {
                logger.error("Cant send Serverpush-Message to the Client", e);
              }
            }

            @Override
            public void finish(Object obj) {
              try {
                String answer = encoder.encodeResponseForSuccess(rpcRequest.getMethod(), obj,
                    rpcRequest.getSerializationPolicy(), rpcRequest.getFlags());
                answerer.send("f" + answer);
              } catch (SerializationException e) {
                logger.error("Cant send Serverpush-Message to the Client", e);
              }
            }

            @Override
            public void abort(Exception caught) {
              try {
                String answer = encoder.encodeResponseForFailure(rpcRequest.getMethod(), caught,
                    rpcRequest.getSerializationPolicy(), rpcRequest.getFlags());
                answerer.send("e" + answer);
              } catch (SerializationException e) {
                logger.error("Cant send Serverpush-Message to the Client", e);
              }
            }
          };
        }
        CancelHandler handler = (CancelHandler) toCall.invoke(servlet, params);

        assert (handler != null) : "Illegal Serverpush Method. Needed " + CancelHandler.class.getName() + " "
            + rpcRequest.getMethod().getName() + "(" + Arrays.toString(callTypes) + ")";

        // TODO Send id to cancel?
        this.handler = handler;

        // String answer = RPC.invokeAndEncodeResponse(servlet, rpcRequest.getMethod(),
        // rpcRequest.getParameters(),
        // rpcRequest.getSerializationPolicy(), rpcRequest.getFlags());
        // answerer.send(answer);
      } catch (Throwable e) {
        logger.error("Cant Process Request because of thrown Exception at " + service + " with data " + data + ":", e);
        return;
      } finally {
        helper.setThreadLocals(servlet, null);
      }
    } else {
      assert (false) : "Wrong protocol:" + data;
    }
  }
}

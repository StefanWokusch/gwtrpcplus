package de.joe.core.rpc.server;

import java.lang.reflect.Method;

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

  @Override
  public String getRequestTypeName() {
    return "p";
  }

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

  @Override
  public void process(String service, String data, HttpServletRequest request, final RequestMethodAnswerer answerer) {
    if (data.startsWith("c")) {
      // TODO Cancel it
      handler.onCancel();
    } else if (data.startsWith("s")) {
      data = data.substring(1);
      start(service, data, request, answerer);
    } else {
      throw new IllegalArgumentException("Wrong protocol:" + data);
    }
  }

  @SuppressWarnings("rawtypes")
  private void start(String service, String data, HttpServletRequest request, final RequestMethodAnswerer answerer) {
    RemoteServiceServlet servlet = helper.getServlet(service);

    // Hack for Jetty Bug
    final ClassLoader oldclassloader = Thread.currentThread().getContextClassLoader();
    helper.setThreadLocals(servlet, request);
    Thread.currentThread().setContextClassLoader(servlet.getClass().getClassLoader());
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

      Method toCall;
      try {
        toCall = servlet.getClass().getMethod(rpcRequest.getMethod().getName(),
            createNeededParameters(rpcRequest.getMethod()));
      } catch (NoSuchMethodException e) {
        logger.error(toIllegalProtocolString(servlet, rpcRequest));
        return;
      }

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
              logger.error("Can't send Serverpush-Message to the Client", e);
            } catch (Throwable e) {
              logger.error("Can't send Serverpush-Message to the Client", e);
            }
          }
        };
      }
      CancelHandler handler = (CancelHandler) toCall.invoke(servlet, params);

      if (handler == null)
        throw new IllegalArgumentException("Illegal implementation: " + toNeededMethodString(rpcRequest.getMethod())
            + " returned null");

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
      Thread.currentThread().setContextClassLoader(oldclassloader);
    }
  }

  private String toIllegalProtocolString(RemoteServiceServlet servlet, final RPCRequest rpcRequest) {
    return "Illegal ServerPush protocol, needed \"" + toNeededMethodString(rpcRequest.getMethod()) + "\" in "
        + servlet.getClass();
  }

  private Class<?>[] createNeededParameters(final Method orgMethod) {
    Class<?>[] callTypes = new Class<?>[orgMethod.getParameterTypes().length + 1];
    {
      int i = 0;
      for (Class<?> c : orgMethod.getParameterTypes())
        callTypes[i++] = c;
      callTypes[i] = ReturnHandler.class;
    }
    return callTypes;
  }

  private String toNeededMethodString(Method orgMethod) {
    StringBuffer params = new StringBuffer();
    int i = 0;
    for (Class<?> c : createNeededParameters(orgMethod)) {
      if (params.length() > 0)
        params.append(", ");
      params.append(c.getName());
      params.append(" arg");
      params.append(i++);
    }
    return CancelHandler.class.getName() + " " + orgMethod.getName() + "(" + params + ")";
  }
}

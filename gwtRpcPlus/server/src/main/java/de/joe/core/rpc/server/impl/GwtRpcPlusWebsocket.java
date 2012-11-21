package de.joe.core.rpc.server.impl;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.FutureCallback;
import org.eclipse.jetty.websocket.core.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.core.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.core.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.core.annotations.WebSocket;
import org.eclipse.jetty.websocket.core.api.UpgradeRequest;
import org.eclipse.jetty.websocket.core.api.UpgradeResponse;
import org.eclipse.jetty.websocket.core.api.WebSocketConnection;
import org.eclipse.jetty.websocket.server.WebSocketCreator;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.eclipse.jetty.websocket.server.WebSocketServlet;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Singleton;

import de.joe.core.rpc.server.RpcManagerServer;
import de.joe.core.rpc.server.RpcManagerServer.AnswerHandler;

@Singleton
public class GwtRpcPlusWebsocket extends WebSocketServlet {
  private static final long serialVersionUID = 1L;

  private final static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GwtRpcPlusWebsocket.class);

  @Inject
  private Provider<GwtRpcSocket> provider;

  private final ThreadLocal<HttpServletRequest> requests = new ThreadLocal<>();

  @Override
  protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    requests.set(request);
    try {
      super.service(request, response);
    } finally {
      requests.set(null);
    }
  }

  @Override
  public void configure(WebSocketServerFactory factory) {
    factory.register(GwtRpcPlusWebsocket.class);
    factory.setCreator(new WebSocketCreator() {
      @Override
      public Object createWebSocket(UpgradeRequest req, UpgradeResponse resp) {
        return provider.get().init(requests.get());
      }
    });
    // Fix not working with Jetty 9.0.0-M1 and Chrome 23-25A
    factory.getExtensionRegistry().unregister("x-webkit-deflate-frame");
  }

  @WebSocket
  public static class GwtRpcSocket {
    // private HttpServletRequestGwtRpc request;
    private String clientId;
    private String permutationStrongName;
    private String moduleBasePath;
    private WebSocketConnection connection;
    private final RpcManagerServer manager;
    private HandlerRegistration handlerReg;

    // private GwtRpcProcessor.AnswererPlus answerer = new GwtRpcProcessor.AnswererPlus() {
    // @Override
    // public void answer(int updateId, String serializedData) {
    // // System.out.println("answering +"+serializedData);
    // synchronized (connection) {
    // try {
    // connection.write(null, new FutureCallback(), "+" + updateId + "#" + serializedData);
    // } catch (IOException e) {
    // logger.error("Exception while Sending Message. This could caused by disconnecting.");
    // }
    // }
    // }
    // };

    @Inject
    public GwtRpcSocket(/* @ShortRunningTasks */ExecutorService executor, RpcManagerServer manager) {
      this.manager = manager;
    }

    // @Override
    public GwtRpcSocket init(HttpServletRequest request) {
      assert (request != null) : "No Request found";
      // this.request = new HttpServletRequestGwtRpc(request);
      return this;
    }

    // @Override
    @OnWebSocketConnect
    public void onOpen(WebSocketConnection connection) {
      if (logger.isInfoEnabled())
        logger.info("Client connected: " + connection);
      this.connection = connection;
    }

    @OnWebSocketMessage
    public void onMessage(final String data) {
      if (logger.isTraceEnabled())
        logger.trace("Data recieved: " + data);
      if (!isInit) {
        isInit = true;
        processInit(data);
      } else {
        processServletCall(data);
      }
    }

    // private boolean isConnected = true;

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
      // isConnected = false;
      if (logger.isInfoEnabled())
        logger.info("Client disconnected " + connection + ": " + reason + " (code: " + statusCode + ")");
      handlerReg.removeHandler();
    }

    private boolean isInit = false;

    private void processInit(String data) {
      clientId = data.substring(0, data.indexOf("#"));
      permutationStrongName = data.substring(0, data.indexOf("#"));
      moduleBasePath = data.substring(data.indexOf("#") + 1);
      if (logger.isDebugEnabled())
        logger.debug("Client initialized with PermutationStrongName: \"" + permutationStrongName
            + "\" modulBasePath:\"" + moduleBasePath + "\"");
      manager.addHandler(clientId, new AnswerHandler() {
        @SuppressWarnings({
            "rawtypes", "unchecked"
        })
        @Override
        public boolean onAnswer(String answer) {
          if (connection.isOpen()) {
            synchronized (connection) {
              try {
                connection.write(null, new FutureCallback(), answer);
              } catch (IOException e) {
                logger.error("Exception while Sending Message. This could caused by disconnecting.");
              }
            }
          }
          return false;
        }
      });
    }

    private void processServletCall(String data) {
      // Hack for Jetty Bug
      // final ClassLoader oldclassloader = Thread.currentThread().getContextClassLoader();
      // try {
      // final String id = data.substring(0, data.indexOf("#"));
      // data = data.substring(data.indexOf("#") + 1);
      // final String service = data.substring(0, data.indexOf("#"));
      // data = data.substring(data.indexOf("#") + 1);

      manager.onCall(clientId, data, permutationStrongName, moduleBasePath);

      // Thread.currentThread().setContextClassLoader(processor.getServletClass(service).getClassLoader());

      // if (request.getContextPath() == null) {
      // logger.warn("Ignoring ServletCall because of no ContextPath set. This could caused in Serverrestart");
      // return;
      // }
      // String response;
      // try {
      // response = id + "#" + processor.process(service, data, request, answerer);
      // } catch (SerializationException e) {
      // response = id + "#ERROR: " + e.getMessage();
      // }

      // if (!isConnected) {
      // logger.info("Answer not send, because of clients disconnect");
      // } else {
      // if (logger.isTraceEnabled())
      // logger.trace("Data sended  : " + response);
      // synchronized (connection) {
      // connection.write(null, new FutureCallback(), response);
      // }
      // }
      // } catch (IOException e) {
      // logger.error("Exception while Sending Message. This could caused by disconnecting.");
      // } finally {
      // Thread.currentThread().setContextClassLoader(oldclassloader);
      // }
    }
  }
}

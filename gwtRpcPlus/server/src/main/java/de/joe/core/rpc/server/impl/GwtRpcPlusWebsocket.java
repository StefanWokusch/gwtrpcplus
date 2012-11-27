package de.joe.core.rpc.server.impl;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.api.WebSocketConnection;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.extensions.ExtensionConfig;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

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
  public void configure(WebSocketServletFactory factory) {
    factory.register(GwtRpcPlusWebsocket.class);
    factory.setCreator(new WebSocketCreator() {
      @Override
      public Object createWebSocket(UpgradeRequest req, UpgradeResponse resp) {
        // Stop Webkit by connecting with deflateFrame because of bug in Jetty 9.0.0.M3
        for (ExtensionConfig e : req.getExtensions())
          if (e.getName().equals("x-webkit-deflate-frame"))
            throw new RuntimeException("Cant support deflate frames");
        return provider.get().init(requests.get());
      }
    });
    // Fix failed with a nullpointer exception at Jetty 9.0.0.M3
    // Fix not working with Jetty 9.0.0.M2 and Chrome 23-25A
    // factory.getExtensionFactory().unregister("x-webkit-deflate-frame");
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
      System.out.println("Recieving " + data);
      if (logger.isTraceEnabled())
        logger.trace("Data recieved: " + data);
      if (!isInit) {
        isInit = true;
        processInit(data);
      } else {
        manager.onCall(clientId, data, permutationStrongName, moduleBasePath);
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
        @Override
        public boolean onAnswer(String answer) {
          if (connection.isOpen()) {
            synchronized (connection) {
              try {
                System.out.println("Writing " + answer.length() + "chars: " + answer.substring(0, 10) + "...");
                connection.write(answer);
              } catch (IOException e) {
                logger.error("Exception while Sending Message. This could caused by disconnecting.");
              }
            }
          }
          return false;
        }
      });
    }
  }
}

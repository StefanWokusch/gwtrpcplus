package com.googlecode.gwtrpcplus.server.servlet;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
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
import com.googlecode.gwtrpcplus.server.impl.RpcManagerServer;
import com.googlecode.gwtrpcplus.server.impl.RpcPlusClient.RpcPlusClientHandler;
import com.googlecode.gwtrpcplus.server.util.Logger;


@Singleton
public class GwtRpcPlusWebsocket extends WebSocketServlet {
  private static final long serialVersionUID = 1L;

  private final static Logger logger = new Logger(GwtRpcPlusWebsocket.class);

  @Inject
  private Provider<GwtRpcSocket> provider;

  private final ThreadLocal<HttpServletRequest> currentRequest = new ThreadLocal<>();

  @Override
  protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    currentRequest.set(request);
    try {
      super.service(request, response);
    } finally {
      currentRequest.remove();
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
        return provider.get().init(currentRequest.get().getContextPath());
      }
    });
    // Jetty Bug #395444 https://bugs.eclipse.org/bugs/show_bug.cgi?id=395444
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
    private Session connection;
    // private ReadWriteLock lock = new ReentrantReadWriteLock();
    private final RpcManagerServer manager;
    private HandlerRegistration handlerReg;
    private String contextPath;

    @Inject
    public GwtRpcSocket(/* @ShortRunningTasks */ExecutorService executor, RpcManagerServer manager) {
      this.manager = manager;
    }

    public GwtRpcSocket init(String contextPath) {
      this.contextPath = contextPath;
      return this;
    }

    // @Override
    @OnWebSocketConnect
    public void onOpen(Session connection) {
      logger.info("Client connected: {}", connection);
      this.connection = connection;
    }

    @OnWebSocketMessage
    public void onMessage(final String data) {
      // System.out.println("Recieving " + data);
      logger.trace("Data recieved: {}", data);
      if (!isInit) {
        isInit = true;
        processInit(data);
      } else {
        manager.onCall(clientId, data, contextPath, permutationStrongName, moduleBasePath);
      }
    }

    // private boolean isConnected = true;

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
      // isConnected = false;
      logger.info("Client disconnected {}: {} (code: {})", connection, reason, statusCode);
      if (handlerReg != null)
        handlerReg.removeHandler();
    }

    private boolean isInit = false;

    private void processInit(String data) {
      clientId = data.substring(0, data.indexOf("#"));
      permutationStrongName = data.substring(0, data.indexOf("#"));
      moduleBasePath = data.substring(data.indexOf("#") + 1);
      logger.debug("Client initialized with PermutationStrongName: \"{} \" modulBasePath:\"{}\"",
          permutationStrongName, moduleBasePath);
      handlerReg = manager.addHandler(clientId, new RpcPlusClientHandler() {
        @Override
        public boolean onAnswer(String answer) {
          answer = answer + "\n";
          if (connection.isOpen()) {
            try {
              logger.trace("send: {}", answer);
              connection.getRemote().sendString(answer);
            } catch (Throwable e) {
              // TODO: handle exception
              logger.error("Exception while Sending Message. This could caused by disconnecting.", e);
            }
          }
          return false;
        }
      });
    }
  }
}

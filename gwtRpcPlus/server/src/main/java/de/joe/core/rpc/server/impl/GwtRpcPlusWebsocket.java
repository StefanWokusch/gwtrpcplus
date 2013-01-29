package de.joe.core.rpc.server.impl;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
//    factory.getExtensionFactory().unregister("x-webkit-deflate-frame");
  }

  @WebSocket
  public static class GwtRpcSocket {
    // private HttpServletRequestGwtRpc request;
    private String clientId;
    private String permutationStrongName;
    private String moduleBasePath;
    private WebSocketConnection connection;
    private ReadWriteLock lock = new ReentrantReadWriteLock();
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
    public void onOpen(WebSocketConnection connection) {
      if (logger.isInfoEnabled())
        logger.info("Client connected: " + connection);
      this.connection = connection;
    }

    @OnWebSocketMessage
    public void onMessage(final String data) {
      // System.out.println("Recieving " + data);
      if (logger.isTraceEnabled())
        logger.trace("Data recieved: " + data);
      if (!isInit) {
        isInit = true;
        processInit(data);
      } else {
        manager.onCall(clientId, contextPath, data, permutationStrongName, moduleBasePath);
      }
    }

    // private boolean isConnected = true;

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
      // isConnected = false;
      if (logger.isInfoEnabled())
        logger.info("Client disconnected " + connection + ": " + reason + " (code: " + statusCode + ")");
      if (handlerReg != null)
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
          answer = answer + "\n";
          if (connection.isOpen()) {
            lock.writeLock().lock();
            try {
              int maxSendSize = 100;
              for (int i = 0; i < answer.length(); i = Math.min(i + maxSendSize, answer.length())) {
                String a = answer.substring(i, Math.min(i + maxSendSize, answer.length()));
                System.out.println("Writing " + a.length() + "chars: " + a.substring(0, 10) + "...");
                Future<Void> future = connection.write(a);
                System.out.println(".");
                future.get();
                System.out.println("ok");
              }
              // System.out.println("send: "+answer);
              // System.out.println("send");
              // Future<Void> future = connection.write(answer);
              // System.out.println("send2");
              // future.get();
              // System.out.println("sendEND");
            } catch (Throwable e) {
              // TODO: handle exception
              logger.error("Exception while Sending Message. This could caused by disconnecting.");

            } finally {
              lock.writeLock().unlock();
            }
          }
          return false;
        }
      });
    }
  }
}

package com.googlecode.gwtrpcplus.server.servlet;

import java.util.HashMap;

import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import com.google.gwt.event.shared.HandlerRegistration;
import com.googlecode.gwtrpcplus.server.GwtRpcPlusWebsocketFilter;
import com.googlecode.gwtrpcplus.server.internal.RpcManagerServer;
import com.googlecode.gwtrpcplus.server.internal.RpcPlusClient.RpcPlusClientHandler;
import com.googlecode.gwtrpcplus.server.internal.util.Logger;

public class GwtRpcPlusWebsocket extends Endpoint {
  private final static Logger logger = new Logger(GwtRpcPlusWebsocket.class);


  private final HashMap<String, GwtRpcSocket> openSockets = new HashMap<>();

  @Override
  public void onOpen(Session session, EndpointConfig config) {
    HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
    String contextPath = (String) config.getUserProperties().get(GwtRpcPlusWebsocketFilter.CONTEXT_PATH_NAME);

    RpcManagerServer manager = (RpcManagerServer) config.getUserProperties().get(RpcManagerServer.class.getName());

    GwtRpcSocket socket = new GwtRpcSocket(manager);
    socket.init(session, httpSession, contextPath);
    session.addMessageHandler(socket);
    openSockets.put(session.getId(), socket);
  }

  @Override
  public void onClose(Session session, CloseReason closeReason) {
    GwtRpcSocket socket = openSockets.remove(session.getId());
    if (socket == null)
      throw new IllegalStateException("Socket not in the openSocket-list");
    socket.onClose(closeReason);
  }

  public static class GwtRpcSocket implements MessageHandler.Partial<String> {
    // private HttpServletRequestGwtRpc request;
    private String clientId;
    private String permutationStrongName;
    private String moduleBasePath;
    private Session session;
    private HttpSession httpSession;
    // private ReadWriteLock lock = new ReentrantReadWriteLock();
    private final RpcManagerServer manager;
    private HandlerRegistration handlerReg;
    private String contextPath;

    public GwtRpcSocket(RpcManagerServer manager) {
      this.manager = manager;
    }

    public void init(Session session, HttpSession httpSession, String contextPath) {
      this.session = session;
      this.httpSession = httpSession;
      this.contextPath = contextPath;
    }

    private boolean isInit = false;

    @Override
    public void onMessage(String data, boolean last) {
      logger.trace("Data recieved: {}", data);
      if (!isInit) {
        isInit = true;
        processInit(data);
      } else {
        manager.onCall(clientId, data, contextPath, permutationStrongName, moduleBasePath, httpSession);
      }
    }

    public void onClose(CloseReason closeReason) {
      // isConnected = false;
      logger.info("Client disconnected {}: {} (code: {})", session, closeReason.getReasonPhrase(),
          closeReason.getCloseCode().getCode());
      if (handlerReg != null)
        handlerReg.removeHandler();
    }

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
          if (session.isOpen()) {
            try {
              logger.trace("send: {}", answer);
              session.getAsyncRemote().sendText(answer);
              return true;
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

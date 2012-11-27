package de.joe.core.rpc.client.impl;

import com.google.gwt.core.client.GWT;

import de.joe.core.rpc.client.RpcManagerClient;
import de.joe.core.rpc.client.util.Client;
import de.joe.core.rpc.client.websocket.WebSocket.Callback;
import de.joe.core.rpc.client.websocket.WebSocketKeepOnline;


public class ConnectionWebsocket extends AbstractConnection {

  /**
   * The websocket keeps connecting itselve, so don't connect while connecting
   */
  private boolean connecting = false;

  @Override
  public void connect() {
    if (websocket.isSupported() && !connecting) {
      connecting = true;
      websocket.connect(GWT.getModuleBaseURL().replace("http:", "ws:").replace("https:", "wss:")
          + "gwtRpcPlusWebsocket");
    }
  }

  @Override
  public void disconnect() {
    websocket.disconnect();
    connecting = false;
    onDisconnect();
  }

  private final WebSocketKeepOnline websocket;

  public ConnectionWebsocket() {
    websocket = new WebSocketKeepOnline(callback);
  }

  private final Callback callback = new Callback() {

    @Override
    public void onOpen() {
      RpcManagerClient.log("Websocket opened");
      websocket.send(Client.id + "#" + GWT.getPermutationStrongName() + "#" + GWT.getModuleBaseURL());
      try {
        onConnected();
      } catch (Throwable e) {
        // TODO Remove this
        e.printStackTrace();
      }
    }

    @Override
    public void onMessage(String message) {
      onRecieve(message);
    }

    @Override
    public void onError(Object e) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onClose() {
      RpcManagerClient.log("Websocket closed");
      onDisconnect();
    }
  };

  @Override
  public void send(String request) {
    websocket.send(request);
  }
}

package com.googlecode.gwtrpcplus.client.connection;

import com.google.gwt.core.client.GWT;
import com.googlecode.gwtrpcplus.client.RpcManagerClient;
import com.googlecode.gwtrpcplus.client.impl.AbstractConnection;
import com.googlecode.gwtrpcplus.client.util.Client;
import com.googlecode.gwtrpcplus.client.util.websocket.WebSocketKeepOnline;
import com.googlecode.gwtrpcplus.client.util.websocket.WebSocket.Callback;

public class ConnectionWebsocket extends AbstractConnection {

  /**
   * The websocket keeps connecting itselve, so don't connect while connecting
   */
  private boolean connecting = false;

  private final String moduleBaseUrl;

  @Override
  public String toString() {
    return getClass().getName();
  }

  @Override
  public void connect() {
    if (websocket.isSupported() && !connecting) {
      connecting = true;
      websocket.connect(moduleBaseUrl.replace("http:", "ws:").replace("https:", "wss:") + "gwtRpcPlusWebsocket");
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
    this(GWT.getModuleBaseURL());
  }

  public ConnectionWebsocket(String moduleBaseUrl) {
    this.moduleBaseUrl = moduleBaseUrl;
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

    private StringBuffer buffer = new StringBuffer();

    @Override
    public void onMessage(String message) {
    	RpcManagerClient.log("recieve message:" + message.length()+" bytes:: "+message);
      buffer.append(message);
      if (message.endsWith("\n")) {
      	RpcManagerClient.log("recieve message:" + message.length()+" bytes");
        onRecieve(buffer.toString());

        buffer = new StringBuffer();
      }
    }

    @Override
    public void onError() {
      RpcManagerClient.log("Websocket error");
    }

    @Override
    public void onClose(int code, String reason) {
      RpcManagerClient.log("Websocket closed " + code + ": " + reason);
      onDisconnect();
      // TODO correct?
      buffer = new StringBuffer();
    }
  };

  @Override
  public void send(String request) {
    websocket.send(request);
  }

  @Override
  public void setPending(boolean pending) {
    // No need in websockets, its supported in the websockets inner-protocoll automatically
  }
}

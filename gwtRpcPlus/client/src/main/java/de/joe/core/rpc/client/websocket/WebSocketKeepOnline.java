package de.joe.core.rpc.client.websocket;

import com.google.gwt.user.client.Timer;

public class WebSocketKeepOnline {
  private WebSocket.Callback callback;
  private String serverUrl = null;

  private final WebSocket websocket = new WebSocket(new WebSocket.Callback() {
    @Override
    public void onMessage(String message) {
      callback.onMessage(message);
    }

    @Override
    public void onClose() {
      webSocketConnectorTimer.schedule(100);
      callback.onClose();
    }

    @Override
    public void onOpen() {
      callback.onOpen();
    }

    public void onError(Object e) {
      callback.onError(e);
    }
  });

  public WebSocketKeepOnline(WebSocket.Callback callback) {
    this.callback = callback;
  }

  public void send(String message) {
    assert (websocket.isConnected()) : "You can only call send when the Websocket is connected";
    websocket.send(message);
  }

  public void connect(String serverUrl) {
    this.serverUrl = serverUrl;
    if (WebSocket.isSupported())
      webSocketConnectorTimer.run();
  }

  public void disconnect() {
    this.serverUrl = null;
    webSocketConnectorTimer.cancel();
  }

  public boolean isConnected() {
    return websocket.isConnected();
  }

  public boolean isSupported() {
    return WebSocket.isSupported();
  }


  /**
   * Try to connect to the Server
   * 
   * Call connectToServer to start it
   */
  private final Timer webSocketConnectorTimer = new Timer() {
    @Override
    public void run() {
      if (!websocket.isConnected()) {
        websocket.connect(serverUrl);
      }
    }
  };
}

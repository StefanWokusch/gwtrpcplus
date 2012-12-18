package de.joe.core.rpc.client.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;

import de.joe.core.rpc.client.util.Client;

public class ConnectionHttp extends AbstractConnection {

  /**
   * Thue when the BasicConnectino should be used
   */
  private boolean connnected = false;

  /**
   * true when the Serverpush-request is pending
   */
  private boolean serverCurrentlyPending = false;


  public boolean isPolling() {
    return serverCurrentlyPending;
  }

  /**
   * true when Response of the Server is expected
   */
  private boolean requestsPending = false;

  /**
   * Amount of pending simple Callbacks (they can get multiple responses, so no serverpolling is
   * needed)
   */
  private int callbacksPending = 0;

  private void updateServerPush() {
    if (requestsPending && connnected && !serverCurrentlyPending && callbacksPending == 0)
      try {
        serverCurrentlyPending = true;
        // System.out.println("Sending longpoll");
        longPushService.sendRequest("", longPushCallback);
      } catch (RequestException e) {
        e.printStackTrace();
      }
  }

  @Override
  public void setPending(boolean pending) {
    this.requestsPending = pending;
    updateServerPush();
  }


  @Override
  public void connect() {
    connnected = true;
    updateServerPush();
    // Always connected
    onConnected();
  }

  @Override
  public void disconnect() {
    connnected = false;
    onDisconnect();
  }

  private final RequestCallback longPushCallback = new RequestCallback() {
    @Override
    public void onResponseReceived(Request request, Response response) {
      // System.out.println("recieved longpoll");
      serverCurrentlyPending = false;

      if (response.getStatusCode() != Response.SC_OK) {
        if (response.getStatusCode() != 0)// Ignore 0 (called by server don't responsed)
          System.err.println("Server responsed " + response.getStatusCode() + ": " + response.getStatusText());
      } else {
        String[] resp = response.getText().split("\n");
        for (String res : resp)
          if (!res.isEmpty())
            onRecieve(res);
      }

      updateServerPush();
    }

    @Override
    public void onError(Request request, Throwable exception) {
      System.err.println("Error at the HTTPConnections longpoll");
      exception.printStackTrace();

      serverCurrentlyPending = false;
      updateServerPush();
    }
  };

  private final RequestCallback callback = new RequestCallback() {
    @Override
    public void onResponseReceived(Request request, Response response) {
      if (response.getStatusCode() != Response.SC_OK) {
        if (response.getStatusCode() != 0)// Ignore 0 (called by server don't responsed)
          System.err.println("Server responsed " + response.getStatusCode() + ": " + response.getStatusText());
      } else
        onRecieve(response.getText());

      callbacksPending--;
      updateServerPush();
    }

    @Override
    public void onError(Request request, Throwable exception) {
      System.err.println("Error at the HTTPConnections callback");
      exception.printStackTrace();

      callbacksPending--;
      updateServerPush();
    }
  };

  private final RequestBuilder service;
  private final RequestBuilder longPushService;

  public ConnectionHttp() {
    service = new RpcRequestBuilder().create(GWT.getModuleBaseURL() + "gwtRpcPlusBasic").finish();
    service.setHeader("clientId", Client.id);
    longPushService = new RpcRequestBuilder().create(GWT.getModuleBaseURL() + "gwtRpcPlusBasic").finish();
    longPushService.setHeader("clientId", Client.id);
    longPushService.setHeader("longpush", "true");
  }

  @Override
  public void send(String request) {
    try {
      service.sendRequest(request, callback);
      callbacksPending++;
    } catch (RequestException e) {
      e.printStackTrace();
    }
  }

}

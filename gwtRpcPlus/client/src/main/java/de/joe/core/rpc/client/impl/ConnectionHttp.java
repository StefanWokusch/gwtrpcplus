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

  private boolean connnected = false;

  private boolean pending = false;

  @Override
  public void connect() {
    connnected = true;
    if (!pending)
      try {
        pending = true;
        longPushService.sendRequest("", longPushCallback);
      } catch (RequestException e) {
        e.printStackTrace();
      }
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
      pending = false;

      if (response.getStatusCode() != Response.SC_OK)
        System.err.println("Server responsed " + response.getStatusCode() + ": " + response.getStatusText());
      else {
        String[] resp = response.getText().split("\n");
        for (String res : resp)
          if (!res.isEmpty())
            onRecieve(res);
      }

      if (connnected) {
        try {
          pending = true;
          longPushService.sendRequest("", longPushCallback);
        } catch (RequestException e) {
          e.printStackTrace();
        }
      }
    }

    @Override
    public void onError(Request request, Throwable exception) {
      // TODO Auto-generated method stub

    }
  };

  private final RequestCallback callback = new RequestCallback() {
    @Override
    public void onResponseReceived(Request request, Response response) {
      if (response.getStatusCode() != Response.SC_OK)
        System.err.println("Server responsed " + response.getStatusCode() + ": " + response.getStatusText());
      else
        onRecieve(response.getText());
    }

    @Override
    public void onError(Request request, Throwable exception) {
      // TODO Auto-generated method stub

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
    } catch (RequestException e) {
      e.printStackTrace();
    }
  }

}

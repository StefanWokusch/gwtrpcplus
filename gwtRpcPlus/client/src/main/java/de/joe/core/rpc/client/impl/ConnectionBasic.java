package de.joe.core.rpc.client.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;

import de.joe.core.rpc.client.util.Client;

public class ConnectionBasic extends AbstractConnection {

  @Override
  public void connect() {
    // Always connected
    onConnected();
  }

  @Override
  public void disconnect() {
    onDisconnect();
  }

  private final RequestCallback callback = new RequestCallback() {
    @Override
    public void onResponseReceived(Request request, Response response) {
      onRecieve(response.getText());
    }

    @Override
    public void onError(Request request, Throwable exception) {
      // TODO Auto-generated method stub

    }
  };

  private final RequestBuilder service;

  public ConnectionBasic() {
    service = new RpcRequestBuilder().create(GWT.getModuleBaseURL() + "gwtRpcPlusBasic").finish();
    service.setHeader("clientId", Client.id);
  }

  @Override
  public void send(String request) {
    try {
      service.sendRequest(request, callback);
    } catch (RequestException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}

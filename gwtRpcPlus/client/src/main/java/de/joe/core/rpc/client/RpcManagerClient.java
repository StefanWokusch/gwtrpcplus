package de.joe.core.rpc.client;

import java.util.HashMap;
import java.util.Map.Entry;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;

import de.joe.core.rpc.client.Connection.RecieveHandler;
import de.joe.core.rpc.client.RequestMethod.ConnectionHandler;
import de.joe.core.rpc.client.RequestMethod.RequestPlus;
import de.joe.core.rpc.client.impl.ConnectionBasic;
import de.joe.core.rpc.client.impl.ConnectionWebsocket;

/**
 * Api the proxies will call
 */
public class RpcManagerClient {
  private static RpcManagerClient instance;

  public static RpcManagerClient get() {
    if (instance == null)
      instance = new RpcManagerClient();
    return instance;
  }

  // TODO make this configurable
  private final ConnectionWebsocket wsconnection = new ConnectionWebsocket();
  private final Connection basicconnection = new ConnectionBasic();

  public RpcManagerClient() {
    wsconnection.setHandler(recieveHandler);
    basicconnection.setHandler(recieveHandler);
  }

  private void send(String request) {
    if (wsconnection.isConnected()) {
      // System.out.println("Sending via Websockets " + request);
      wsconnection.send(request);
    } else {
      // System.out.println("Sending via Http " + request);
      basicconnection.send(request);
    }
  }

  private final RecieveHandler recieveHandler = new RecieveHandler() {
    @Override
    public void onRecieve(String data) {
      final String id = data.substring(0, data.indexOf("#"));
      data = data.substring(data.indexOf("#") + 1);

      RequestPlus request = requests.get(id);
      if (request != null) {
        request.onAnswer(data);
      } else
        System.out.println("Ignoring Answer " + id + ": " + data);
    }

    @Override
    public void onDisconnect() {
      // System.out.println("onDisconnect");
    }

    @Override
    public void onConnected() {
      // System.out.println("onConnected");
    }
  };

  private int currentId = 1;
  private final HashMap<String, RequestPlus> requests = new HashMap<String, RequestPlus>();

  private ConnectionHandler handler = new ConnectionHandler() {
    @Override
    public void addRequest(RequestPlus request) {
      String id = "" + (currentId++);
      requests.put(id, request);
      send(id + "#" + request.getServiceName() + "#" + request.getRequestString());
    }

    @Override
    public void removeRequest(RequestPlus request) {
      for (Entry<String, RequestPlus> e : requests.entrySet())
        if (e.getValue() == request) {
          requests.remove(e.getKey());
          return;
        }
      System.out.println("Warn: Remove a request, that not exist ignored");
    }
  };

  public Request call(RequestMethod method, String requestData, RequestCallback requestCallback) {
    method.setHandler(handler);
    // TODO Remove this Method and replace calls directly to the methods?
    return method.call(requestData, requestCallback);
  }


}

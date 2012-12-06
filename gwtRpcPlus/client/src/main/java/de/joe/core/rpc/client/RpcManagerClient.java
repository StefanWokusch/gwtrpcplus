package de.joe.core.rpc.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;

import de.joe.core.rpc.client.Connection.RecieveHandler;
import de.joe.core.rpc.client.RequestMethod.ConnectionHandler;
import de.joe.core.rpc.client.RequestMethod.RequestPlus;

/**
 * Api the proxies will call
 */
public class RpcManagerClient {
  private static RpcManagerClient instance;

  public static void log(String text) {
    // GWT.log(text);
  }

  public static RpcManagerClient get() {
    if (instance == null)
      instance = new RpcManagerClient((ConnectionProvider) GWT.create(ConnectionProvider.class));
    return instance;
  }

  public enum ConnectionState {
    DISCONNECTED, TRYCONNECT, CONNECTED
  }

  private class ConnectionWrapper {
    public Connection connection;
    public ConnectionState state = ConnectionState.DISCONNECTED;

    public ConnectionWrapper(Connection c) {
      connection = c;
    }
  }

  /**
   * available Connections, sorted by priority (first = highest priority)
   */
  private List<ConnectionWrapper> connections = new ArrayList<ConnectionWrapper>();

  private ConnectionWrapper getActiveConnection() {
    for (ConnectionWrapper w : connections)
      if (w.state == ConnectionState.CONNECTED)
        return w;
    return null;
  }

  public RpcManagerClient(ConnectionProvider prov) {
    for (final Connection c : prov.get()) {
      final ConnectionWrapper wrapper = new ConnectionWrapper(c);
      connections.add(wrapper);
      c.setHandler(new RecieveHandler() {
        @Override
        public void onRecieve(String data) {
          if (data.isEmpty())
            return;
          assert (data.contains("#")) : "Illegal protocol: \"" + data + "\"";
          log("recieve from " + c.getClass() + " " + (data.length() <= 100 ? data : data.subSequence(0, 100)));

          RpcManagerClient.this.onRecieve(data);
        }

        @Override
        public void onDisconnect() {
          if (wrapper.state != ConnectionState.DISCONNECTED) {
            ConnectionWrapper lastActive = getActiveConnection();

            wrapper.state = ConnectionState.DISCONNECTED;
            RpcManagerClient.this.onDisconnect(c);

            if (lastActive != getActiveConnection())
              for (RpcManagerHandler h : handlers)
                h.onActiveConnectionChanged(getActiveConnection().connection);
          }
        }

        @Override
        public void onConnected() {
          assert (wrapper.state == ConnectionState.TRYCONNECT) : "You cant call onconnected when you are "
              + wrapper.state;
          ConnectionWrapper lastActive = getActiveConnection();

          wrapper.state = ConnectionState.CONNECTED;
          RpcManagerClient.this.onConnected(c);

          if (lastActive != getActiveConnection())
            for (RpcManagerHandler h : handlers)
              h.onActiveConnectionChanged(getActiveConnection().connection);
        }
      });
      wrapper.state = ConnectionState.TRYCONNECT;
      c.connect();
    }
  }

  private void send(String request) {
    ConnectionWrapper con = getActiveConnection();
    con.connection.send(request);
  }

  private void onRecieve(String data) {
    final String id = data.substring(0, data.indexOf("#"));
    data = data.substring(data.indexOf("#") + 1);

    RequestPlus request = requests.get(id);
    if (request != null) {
      request.onAnswer(data);
    } else {
      log("Ignoring Answer " + id + ": " + data);
    }
  }

  private void onDisconnect(Connection c) {
    ConnectionWrapper activeConnection = getActiveConnection();
    if (activeConnection == null)
      for (ConnectionWrapper w : connections)
        if (w.state == ConnectionState.DISCONNECTED) {
          w.state = ConnectionState.TRYCONNECT;
          w.connection.connect();
        }
  }

  private void onConnected(Connection c) {
    ConnectionWrapper activeConnection = getActiveConnection();
    assert (activeConnection != null);
    boolean isLowerThanActive = false;
    for (ConnectionWrapper w : connections) {
      isLowerThanActive = isLowerThanActive || w.equals(activeConnection);
      if (isLowerThanActive && !w.equals(activeConnection) && w.state != ConnectionState.DISCONNECTED)
        w.connection.disconnect();
    }
  }

  private int currentId = 1;
  private final HashMap<String, RequestPlus> requests = new HashMap<String, RequestPlus>();

  private ConnectionHandler handler = new ConnectionHandler() {
    @Override
    public void addRequest(RequestPlus request) {
      String id = "" + (currentId++);
      requests.put(id, request);
      send(id + "#" + request.getRequestTypeName() + "#" + request.getServiceName() + "#" + request.getRequestString());
    }

    @Override
    public void removeRequest(RequestPlus request) {
      for (Entry<String, RequestPlus> e : requests.entrySet())
        if (e.getValue() == request) {
          requests.remove(e.getKey());
          return;
        }
      log("Warn: Remove a request, that not exist ignored");
    }
  };

  public Request call(RequestMethod method, String requestData, RequestCallback requestCallback) {
    method.setHandler(handler);
    // TODO Remove this method and replace calls directly to the methods?
    return method.call(requestData, requestCallback);
  }

  /**
   * Handler for state updates
   */
  public static interface RpcManagerHandler {
    void onActiveConnectionChanged(Connection newConnection);
  }

  private final ArrayList<RpcManagerHandler> handlers = new ArrayList<RpcManagerHandler>();

  public HandlerRegistration addHandler(final RpcManagerHandler handler) {
    handlers.add(handler);
    return new HandlerRegistration() {
      @Override
      public void removeHandler() {
        handlers.remove(handler);
      }
    };
  }

  public Connection getCurrentConnection() {
    ConnectionWrapper c = getActiveConnection();
    return c == null ? null : c.connection;
  }
}

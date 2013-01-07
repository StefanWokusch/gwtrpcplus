package de.joe.core.rpc.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;

import de.joe.core.rpc.client.Connection.RecieveHandler;
import de.joe.core.rpc.client.RequestMethod.ConnectionHandler;
import de.joe.core.rpc.client.RequestMethod.RequestPlus;
import de.joe.core.rpc.client.util.MyTimer;

/**
 * Api the proxies will call
 */
public class RpcManagerClient {
  private static RpcManagerClient instance;

  public static void log(String text) {
    System.out.println(text);
    // GWT.log(text);
  }

  protected MyTimer timer;

  private void schedule() {
    if (timer == null) {
      timer = new MyTimer.DefaultTimer() {
        @Override
        public void fire() {
          onTimeout();
        }
      };
    }
    timer.schedule(true);
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
          schedule();
          if (data.isEmpty())
            return;
          assert (data.contains("#")) : "Illegal protocol: \"" + data + "\"";
          log("recieve from " + c.getClass() + " " + (data.length() <= 100 ? data : data.subSequence(0, 100)));

          for (RpcManagerHandler handler : handlers)
            handler.onResponse();

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

        @Override
        public void onTimeout() {
          timer.schedule(false);
        }
      });
      wrapper.state = ConnectionState.TRYCONNECT;
      c.connect();
    }
  }

  private void send(String request) {
    schedule();
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
      send(id, request);
      checkPending();
    }

    @Override
    public void removeRequest(RequestPlus request) {
      for (Entry<String, RequestPlus> e : requests.entrySet())
        if (e.getValue() == request) {
          requests.remove(e.getKey());
          checkPending();
          return;
        }
      log("Warn: Remove a request, that not exist ignored");
    }
  };

  public Request call(RequestMethod method, String requestData, RequestCallback requestCallback) {
    register(method);
    // TODO Remove this method and replace calls directly to the methods?
    return method.call(requestData, requestCallback);
  }

  protected void register(RequestMethod method) {
    method.setHandler(handler);
  }

  /**
   * Handler for state updates
   */
  public static interface RpcManagerHandler {
    /**
     * Called when the currentConnection changed (like websockets connected)
     * 
     * @param newConnection the new and now active Connection
     */
    void onActiveConnectionChanged(Connection newConnection);

    /**
     * Called when some Response recieved
     */
    void onResponse();

    /**
     * Called when some Responses of the Server are expected, but it timedout
     */
    void onTimeout();
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

  private void send(String id, RequestPlus request) {
    send(id + "#" + request.getRequestTypeName() + "#" + request.getServiceName() + "#" + request.getRequestString());
  }

  /**
   * Called when no Answer recieved. This ca
   */
  @SuppressWarnings("unchecked")
  protected void onTimeout() {
    boolean pending = false;
    Set<Entry<String, RequestPlus>> entrySet = requests.entrySet();
    for (Entry<String, RequestPlus> request : entrySet.toArray(new Entry[entrySet.size()])) {
      boolean resend = request.getValue().onTimeout();
      if (resend) {
        pending = true;
        send(request.getKey(), request.getValue());
      }
    }

    if (pending) {
      schedule();
      for (RpcManagerHandler handler : handlers)
        handler.onTimeout();
    }
  }

  private void checkPending() {
    boolean anyRequestPending = isAnyRequestPending();
    for (ConnectionWrapper c : connections)
      c.connection.setPending(anyRequestPending);
  }

  private boolean isAnyRequestPending() {
    return !requests.isEmpty();
  }


}

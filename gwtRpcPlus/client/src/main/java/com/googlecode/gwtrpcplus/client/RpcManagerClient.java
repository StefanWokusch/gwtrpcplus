package com.googlecode.gwtrpcplus.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.googlecode.gwtrpcplus.client.Connection.RecieveHandler;
import com.googlecode.gwtrpcplus.client.RequestMethod.ConnectionHandler;
import com.googlecode.gwtrpcplus.client.RequestMethod.RequestPlus;
import com.googlecode.gwtrpcplus.client.util.TimeoutTimer;
import com.googlecode.gwtrpcplus.client.util.MyWindow;

/**
 * Api the proxies will call
 */
public class RpcManagerClient {
	private static RpcManagerClient instance;

	// TODO Fire onTimeout after first direct Timeout call (to show early server-disconnect)
	// TODO Fire onTimeout early (shorter timer) after first answer after timeout (to resend all
	// requests fast)
	public static void log(String text) {
		// System.err.println(text);
		// GWT.log(text);
	}

	protected TimeoutTimer timer;

	private void schedule() {
		if (timer == null) {
			timer = new TimeoutTimer.DefaultTimer() {
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

	private static class ConnectionWrapper {
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
		this(prov, new MyWindow.DefaultWindow());
	}

	public RpcManagerClient(ConnectionProvider prov, MyWindow window) {
		window.addWindowClosingHandler(new ClosingHandler() {
			@Override
			public void onWindowClosing(ClosingEvent event) {
				send("disconnect");
			}
		});

		for (final Connection c : prov.get()) {
			final ConnectionWrapper wrapper = new ConnectionWrapper(c);
			connections.add(wrapper);
			c.setHandler(new RecieveHandler() {
				@Override
				public void onRecieve(String data) {
					schedule();
					if (data.isEmpty())
						return;
					// Keepalive
					if (data.equals(".\n")) {
						log("keepalive recieved");
						return;
					}
					assert data.contains("#") : "Illegal protocol: \"" + data + "\"";
					log("recieve from " + c.getClass() + " " + (data.length() <= 100 ? data : data.subSequence(0, 100)));

					for (RpcManagerHandler handler : handlers)
						handler.onResponse();

					RpcManagerClient.this.onRecieve(data);
				}

				@Override
				public void onDisconnect() {
					if (wrapper.state == ConnectionState.CONNECTED) {
						ConnectionWrapper lastActive = getActiveConnection();

						wrapper.state = ConnectionState.DISCONNECTED;
						RpcManagerClient.this.onDisconnect();

						if (lastActive != getActiveConnection())
							for (RpcManagerHandler h : handlers)
								h.onActiveConnectionChanged(getActiveConnection().connection);
					}
				}

				@Override
				public void onConnected() {
					assert wrapper.state == ConnectionState.TRYCONNECT : "You can't call onconnected when you are " + wrapper.state;
					// ConnectionWrapper lastActive = getActiveConnection();

					wrapper.state = ConnectionState.CONNECTED;
					RpcManagerClient.this.onConnected();

					// Removed because we need to call this for http-polling state
					// if (lastActive != getActiveConnection())
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
			try {
				request.onAnswer(data);
			} catch (Throwable e) {
				UncaughtExceptionHandler uncaughtExceptionHandler = GWT.getUncaughtExceptionHandler();
				if (uncaughtExceptionHandler == null)
					GWT.log("Exception while processing Answer with no UncaughtExceptionHandler", e);
				else
					uncaughtExceptionHandler.onUncaughtException(e);
			}
		} else {
			log("Ignoring Answer " + id + ": " + data);
		}
	}

	private void onDisconnect() {
		ConnectionWrapper activeConnection = getActiveConnection();
		if (activeConnection == null)
			for (ConnectionWrapper w : connections)
				if (w.state == ConnectionState.DISCONNECTED) {
					w.state = ConnectionState.TRYCONNECT;
					w.connection.connect();
				}
	}

	private void onConnected() {
		ConnectionWrapper activeConnection = getActiveConnection();
		assert activeConnection != null;
		boolean isLowerThanActive = false;
		for (ConnectionWrapper w : connections) {
			isLowerThanActive = isLowerThanActive || w.equals(activeConnection);
			if (isLowerThanActive && !w.equals(activeConnection) && w.state != ConnectionState.DISCONNECTED)
				w.connection.disconnect();
		}
	}

	private int currentId = 0;
	private final HashMap<String, RequestPlus> requests = new HashMap<String, RequestPlus>();

	private final ConnectionHandler handler = new ConnectionHandler() {
		@Override
		public void addRequest(RequestPlus request) {
			String id = "" + (++currentId);
			request.setId(id);
			requests.put(id, request);
			send(id, request);
			checkPending();
		}

		@Override
		public void removeRequest(RequestPlus request) {
			String id = request.getId();
			assert id != null : "No ID saved";
			RequestPlus r = requests.remove(id);
			if (r == null) {
				log("Warn: Remove a request, that not exist ignored");
				return;
			}
			assert request == r;
			checkPending();
			// for (Entry<String, RequestPlus> e : requests.entrySet())
			// if (e.getValue() == request) {
			// requests.remove(e.getKey());
			// return;
			// }
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
	public interface RpcManagerHandler {
		/**
		 * Called when the currentConnection changed (like websockets connected)
		 * 
		 * @param newConnection
		 *          the new and now active Connection
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
	 * Called when no Answer recieved.
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

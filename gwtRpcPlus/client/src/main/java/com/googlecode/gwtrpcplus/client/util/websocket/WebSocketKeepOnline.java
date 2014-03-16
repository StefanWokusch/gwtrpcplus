package com.googlecode.gwtrpcplus.client.util.websocket;

import com.google.gwt.user.client.Timer;

public class WebSocketKeepOnline {
	private WebSocket.Callback callback;
	private String serverUrl = null;

	private final static int TIMEOUT_STEPS[] = { 100, 200, 500, 600, 700, 1000, 1000, 5000 };
	private int timeoutStep = 0;

	private final WebSocket websocket = new WebSocket(new WebSocket.Callback() {
		@Override
		public void onMessage(String message) {
			callback.onMessage(message);
		}

		@Override
		public void onClose(int code, String reason) {
			int timeout = TIMEOUT_STEPS[Math.min(TIMEOUT_STEPS.length - 1, timeoutStep)];
			timeoutStep++;
			webSocketConnectorTimer.schedule(timeout);
		}

		@Override
		public void onOpen() {
			callback.onOpen();
			timeoutStep = 0;
		}

		public void onError() {
			callback.onError();
		}
	});

	public WebSocketKeepOnline(WebSocket.Callback callback) {
		this.callback = callback;
	}

	public void send(String message) {
		assert websocket.isConnected() : "You can only call send when the Websocket is connected";
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

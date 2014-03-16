package com.googlecode.gwtrpcplus.client.util.websocket;

import com.google.gwt.core.client.GWT;

public class WebSocket {
	public interface Callback {
		void onOpen();

		void onClose(int code, String reason);

		void onError();

		void onMessage(String message);
	}

	private final Callback callback;

	protected Object ws;

	public WebSocket(Callback callback) {
		this.callback = callback;
	}

	private boolean isConnected = false;

	protected final void onopen() {
		isConnected = true;
		try {
			callback.onOpen();
		} catch (Throwable e) {
			if (GWT.getUncaughtExceptionHandler() != null)
				GWT.getUncaughtExceptionHandler().onUncaughtException(e);
			else
				GWT.log("Error", e);
		}
	}

	protected final void onclose(int code, String reason) {
		isConnected = false;
		ws = null;

		try {
			callback.onClose(code, reason);
		} catch (Throwable e) {
			if (GWT.getUncaughtExceptionHandler() != null)
				GWT.getUncaughtExceptionHandler().onUncaughtException(e);
			else
				GWT.log("Error", e);
		}
	}

	protected final void onmessage(String message) {
		try {
			callback.onMessage(message);
		} catch (Throwable e) {
			if (GWT.getUncaughtExceptionHandler() != null)
				GWT.getUncaughtExceptionHandler().onUncaughtException(e);
			else
				GWT.log("Error", e);
		}
	}

	protected final void onerror() {
		try {
			callback.onError();
		} catch (Throwable e) {
			if (GWT.getUncaughtExceptionHandler() != null)
				GWT.getUncaughtExceptionHandler().onUncaughtException(e);
			else
				GWT.log("Error", e);
		}
	}

	public static native boolean isSupported()/*-{
		return "WebSocket" in $wnd || "MozWebSocket" in $wnd;
	}-*/;

	public static native int getErrorCode(Object obj)/*-{
		return obj.code;
	}-*/;

	public boolean isConnected() {
		return isSupported() && isConnected;
	}

	public native void connect(String serverUrl) /*-{
		var that = this;

		if ("WebSocket" in $wnd)
			this.@com.googlecode.gwtrpcplus.client.util.websocket.WebSocket::ws = new WebSocket(
					serverUrl);
		else
			this.@com.googlecode.gwtrpcplus.client.util.websocket.WebSocket::ws = new MozWebSocket(
					serverUrl);

		this.@com.googlecode.gwtrpcplus.client.util.websocket.WebSocket::ws.onopen = function() {
			that.@com.googlecode.gwtrpcplus.client.util.websocket.WebSocket::onopen()();
		};
		this.@com.googlecode.gwtrpcplus.client.util.websocket.WebSocket::ws.onmessage = function(
				response) {
			if (response.data) {
				that.@com.googlecode.gwtrpcplus.client.util.websocket.WebSocket::onmessage(Ljava/lang/String;)( response.data );
			}
		};
		this.@com.googlecode.gwtrpcplus.client.util.websocket.WebSocket::ws.onclose = function(
				m) {
			that.@com.googlecode.gwtrpcplus.client.util.websocket.WebSocket::onclose(ILjava/lang/String;)(m.code, m.reason);
		};
		this.@com.googlecode.gwtrpcplus.client.util.websocket.WebSocket::ws.onerror = function(
				m) {
			that.@com.googlecode.gwtrpcplus.client.util.websocket.WebSocket::onerror()();
		};
	}-*/;

	public native void send(String message) /*-{
		if (this.@com.googlecode.gwtrpcplus.client.util.websocket.WebSocket::ws) {
			this.@com.googlecode.gwtrpcplus.client.util.websocket.WebSocket::ws
					.send(message);
		} else {
			alert("not connected!"
					+ this.@com.googlecode.gwtrpcplus.client.util.websocket.WebSocket::ws);
		}
	}-*/;

	public native void close() /*-{
		this.@com.googlecode.gwtrpcplus.client.util.websocket.WebSocket::ws
				.close();
	}-*/;

}
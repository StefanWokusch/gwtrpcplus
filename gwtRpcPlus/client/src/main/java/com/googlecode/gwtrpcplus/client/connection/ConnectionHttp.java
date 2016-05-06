package com.googlecode.gwtrpcplus.client.connection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.googlecode.gwtrpcplus.client.RpcManagerClient;
import com.googlecode.gwtrpcplus.client.impl.AbstractConnection;
import com.googlecode.gwtrpcplus.client.util.Client;

public class ConnectionHttp extends AbstractConnection {

	@Override
	public String toString() {
		return getClass().getName() + (isPolling() ? " Polling" : "");
	}

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
	 * Amount of pending simple Callbacks (they can get multiple responses, so
	 * no serverpolling is needed)
	 */
	private int callbacksPending = 0;

	/**
	 * Flag to not do a serverpush ehen server isnt responding
	 * 
	 * this causes a bug after serverrecover, not timeouting some results,
	 * because the polling reschedule the ontimeout
	 */
	private boolean notresponding = false;

	private void updateServerPush() {
		if (!notresponding && requestsPending && connnected && !serverCurrentlyPending && callbacksPending == 0)
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
			serverCurrentlyPending = false;

			if (response.getStatusCode() != Response.SC_OK) {
				if (response.getStatusCode() == 0) // server don't responsed
					onTimeout();
				else
					System.err
							.println("Server responsed " + response.getStatusCode() + ": " + response.getStatusText());
			} else {
				final String[] resp = response.getText().split("\n");
				// long start = System.currentTimeMillis();
				for (String res : resp)
					onRecieve(res);
				// long duration = (System.currentTimeMillis() - start);
				// System.out.println("Duration: " + duration + "ms (avg:" +
				// duration / resp.length + ")");
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
			int statusCode = response.getStatusCode();
			notresponding = statusCode == 0;
			if (statusCode != Response.SC_OK) {
				if (statusCode != 0)// Ignore 0 (called by server don't
									// responsed)
					System.err.println("Server responsed " + statusCode + ": " + response.getStatusText());
				else
					onTimeout();
			} else {
				final String[] resp = response.getText().split("\n");
				RpcManagerClient.log("Recieved " + resp.length + " Responses in one Call");
				for (String res : resp)
					onRecieve(res);
			}

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
		this(GWT.getModuleBaseURL() + "/");
	}

	protected String getServletName() {
		return "gwtRpcPlusBasic";
	}

	public ConnectionHttp(String moduleBaseUrl) {
		service = new RpcRequestBuilder().create(moduleBaseUrl + getServletName()).finish();
		service.setHeader("clientId", Client.id);
		longPushService = new RpcRequestBuilder().create(moduleBaseUrl + getServletName()).finish();
		longPushService.setHeader("clientId", Client.id);
		longPushService.setHeader("longpush", "true");
	}

	@Override
	public void send(String request) {
		RpcManagerClient.log("request " + request);
		doSend(request);
	}

	protected void doSend(String request) {
		try {
			service.sendRequest(request, callback);
			callbacksPending++;
		} catch (RequestException e) {
			e.printStackTrace();
		}
	}

}

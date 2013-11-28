package com.googlecode.gwtrpcplus.client.widgets;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.googlecode.gwtrpcplus.client.Connection;
import com.googlecode.gwtrpcplus.client.RpcManagerClient;
import com.googlecode.gwtrpcplus.client.RpcManagerClient.RpcManagerHandler;
import com.googlecode.gwtrpcplus.client.connection.ConnectionHttp;
import com.googlecode.gwtrpcplus.client.connection.ConnectionWebsocket;

/**
 * Widget to see the current-Connection
 */
public class ConnectionDebugWidget extends Composite {
	private final HTML connection = new HTML();

	public ConnectionDebugWidget() {
		initWidget(connection);
		connection.setWordWrap(false);

		setConnection(RpcManagerClient.get().getCurrentConnection());

		RpcManagerClient.get().addHandler(new RpcManagerHandler() {
			@Override
			public void onActiveConnectionChanged(Connection newConnection) {
				setConnection(newConnection);
			}

			@Override
			public void onResponse() {
				setConnection(currentConnection);
			}

			@Override
			public void onTimeout() {
				setConnection(currentConnection);
			}
		});
	}

	private Connection currentConnection;

	private void setConnection(Connection currentConnection) {
		this.currentConnection = currentConnection;
		Scheduler.get().scheduleEntry(new ScheduledCommand() {
			@Override
			public void execute() {
				if (ConnectionDebugWidget.this.currentConnection == null) {
					connection.setHTML("&mdash;"); // long dash
				} else if (ConnectionDebugWidget.this.currentConnection instanceof ConnectionWebsocket) {
					connection.setText("Websocket");
				} else if (ConnectionDebugWidget.this.currentConnection instanceof ConnectionHttp) {
					if (((ConnectionHttp) ConnectionDebugWidget.this.currentConnection).isPolling())
						connection.setText("HTTP-Polling");
					else
						connection.setText("HTTP");
				}
			}
		});
	}
}

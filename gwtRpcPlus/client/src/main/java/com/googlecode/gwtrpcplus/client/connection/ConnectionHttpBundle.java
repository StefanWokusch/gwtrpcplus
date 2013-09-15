package com.googlecode.gwtrpcplus.client.connection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.googlecode.gwtrpcplus.client.RpcManagerClient;

public class ConnectionHttpBundle extends ConnectionHttp {

	public ConnectionHttpBundle() {
		super(GWT.getHostPageBaseURL() + GWT.getModuleName() + "/gwtRpcPlusBundle");
	}

	private StringBuilder requestQueue = null;

	@Override
	public void send(String request) {
		RpcManagerClient.log("request scheduled " + request);

		if (requestQueue == null) {
			requestQueue = new StringBuilder();
			Scheduler.get().scheduleDeferred(new ScheduledCommand() {
				@Override
				public void execute() {
					RpcManagerClient.log("sending scheduled requests " + requestQueue.length() + " bytes");
					doSend(requestQueue.toString());
					requestQueue = null;
				}
			});
		}

		requestQueue.append(request.length() + "\n" + request);
	}

}

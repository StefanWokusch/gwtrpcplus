package com.googlecode.gwtrpcplus.client.connection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.googlecode.gwtrpcplus.client.RpcManagerClient;

public class ConnectionHttpBundle extends ConnectionHttp {

	public ConnectionHttpBundle() {
		super();
	}
	public ConnectionHttpBundle(String moduleBaseUrl) {
	  super(GWT.getHostPageBaseURL() + GWT.getModuleName() + "/");
	}
	
	@Override
	protected String getServletName() {
	  return "gwtRpcPlusBundle";
	}

	private int requestAmount = 0;
	private StringBuilder requestBundle = null;

	@Override
	public void send(String request) {
		RpcManagerClient.log("request scheduled " + request);

		if (requestBundle == null) {
			requestBundle = new StringBuilder();
			Scheduler.get().scheduleDeferred(new ScheduledCommand() {
				@Override
				public void execute() {
					send();
				}
			});
		}
		requestBundle.append(request.length() + "\n" + request);
		requestAmount++;

		if (requestAmount >= 20) {
			send();
		}
	}

	private void send() {
		if (requestAmount > 0) {
			RpcManagerClient.log("sending scheduled requests " + requestBundle.length() + " bytes");
			doSend(requestBundle.toString());
			requestBundle = null;
			requestAmount = 0;
		}
	}

}

package com.googlecode.gwtrpcplus.server.internal;

import javax.servlet.ServletContext;

public interface WebsocketSetup {
	public static final String CONTEXT_PATH_NAME = "gwtrpcplus-websocket-servletContextPath";

	boolean init(ServletContext servletContext, String websocketPath, RpcManagerServer manager);
	
}

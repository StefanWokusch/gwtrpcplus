package com.googlecode.gwtrpcplus.server.jetty;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;

import com.googlecode.gwtrpcplus.server.ModuleGwtRpcPlus;
import com.googlecode.gwtrpcplus.server.ModuleGwtRpcPlus.WebsocketModule;
import com.googlecode.gwtrpcplus.server.util.Logger;

public class ModuleGwtRpcPlusWebsocket extends WebsocketModule {
	private final static Logger logger = new Logger(ModuleGwtRpcPlus.class);

	private boolean added = false;

	@Override
	protected void configureServlets() {
		try {
			@SuppressWarnings("unchecked")
			Class<? extends HttpServlet> c = (Class<? extends HttpServlet>) Class
					.forName("com.googlecode.gwtrpcplus.server.jetty.servlet.GwtRpcPlusJettyWebsocket");

			// Check for correct Jetty Version
			if (!getServletContext().getServerInfo().startsWith("jetty/9.")) {
				throw new RuntimeException("Only supported in jetty 9 yet (working since jetty 9.0.0), but was "
						+ getServletContext().getServerInfo());
			}

			// Try adding the WebsocketServlet
			Map<String, String> params = new HashMap<String, String>();
			params.put("bufferSize", "100000");
			serve(getWebsocketPath()).with(c, params);
			added = true;
		} catch (ClassNotFoundException e) {
			// Ignore when not added
		} catch (Throwable e) {
			logger.trace("Ignoring creation the WebSocketServlet. Using only HTTP Calls. Exception", e);
			logger.warn("Ignoring creation the WebSocketServlet. Using only HTTP Calls. Exception:" + e.getClass().getName()
					+ " :: " + e.getMessage());
		}
	}

	@Override
	public boolean isAdded() {
		return added;
	}

}

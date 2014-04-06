package com.googlecode.gwtrpcplus.server;

import javax.servlet.ServletException;

import com.googlecode.gwtrpcplus.server.internal.WebsocketSetup;
import com.googlecode.gwtrpcplus.server.internal.util.Logger;

public class GwtRpcPlusWebsocketFilter extends GwtRpcPlusFilter {
	private final static Logger logger = new Logger(GwtRpcPlusWebsocketFilter.class);

	@Override
	protected void initWebsocket() throws ServletException {
		boolean added = false;
		WebsocketSetup setup = null;
		try {
			setup = createSetup();
		} catch (Exception | NoClassDefFoundError e) {
			logger.warn("No JSR-356 Websocket-Support found for " + getServletContext().getServerInfo(), e);
		}
		if (setup != null)
			added = setup.init(getServletContext(), getWebsocketPath(), manager);
		// Add dummy if not supported
		if (!added)
			super.initWebsocket();
	}

	protected WebsocketSetup createSetup() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		return (WebsocketSetup) Class.forName("com.googlecode.gwtrpcplus.server.internal.WebsocketSetupImpl").newInstance();
	}

	private String getWebsocketPath() {
		String modulename = getModuleName();
		if (modulename == null)
			throw new IllegalStateException("A modulename is needed to use Websocket-Extension of GwtRpcPlus");
		return "/" + modulename + "/gwtRpcPlusWebsocket";
	}
}

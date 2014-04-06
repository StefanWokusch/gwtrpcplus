package com.googlecode.gwtrpcplus.server;

import com.googlecode.gwtrpcplus.server.internal.WebsocketSetup;

/**
 * WebsocketFilter for some special Environments with possible problems with ClassLoaders like OSGI.
 */
public class GwtRpcPlusWebsocketFilterStrict extends GwtRpcPlusWebsocketFilter {
	protected WebsocketSetup createSetup() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		return new com.googlecode.gwtrpcplus.server.internal.WebsocketSetupImpl();
	}
}

package com.googlecode.gwtrpcplus.server.internal.type;

import javax.servlet.http.HttpServletRequest;

import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.googlecode.gwtrpcplus.server.internal.util.Logger;
import com.googlecode.gwtrpcplus.server.internal.util.RpcHelper;

public class RequestMethodHandlerBasic implements RequestMethodHandler {
	private final static Logger logger = new Logger(RequestMethodHandlerBasic.class);

	private final RpcHelper helper;

	@Override
	public String getRequestTypeName() {
		return "b";
	}

	public RequestMethodHandlerBasic(RpcHelper helper) {
		this.helper = helper;
	}

	@Override
	public void process(String service, String data, HttpServletRequest request, RequestMethodAnswerer answerer) {
		// Hack for Webserver Bugs
		// final ClassLoader oldclassloader = Thread.currentThread().getContextClassLoader();

		RemoteServiceServlet servlet = helper.getServlet(service);
		helper.setThreadLocals(servlet, request);
		// Thread.currentThread().setContextClassLoader(servlet.getClass().getClassLoader());
		try {
			final RPCRequest rpcRequest;
			try {
				rpcRequest = RPC.decodeRequest(data, servlet.getClass(), servlet);
			} catch (NullPointerException e) {
				// This is only to avoid long exceptionstacks in logging
				// TODO fix this nullpointerbug instead
				logger.warn("Cant Process Request, because of not loaded Policies. This could caused by a Serverrestart.");
				logger.trace("Cant Process Request, because of not loaded Policies. This could caused by a Serverrestart.", e);
				return;
			} catch (IllegalStateException e) {
				throw new IllegalStateException("Exception while decoding Request of " + servlet.getClass(), e);
			}
			String answer = RPC.invokeAndEncodeResponse(servlet, rpcRequest.getMethod(), rpcRequest.getParameters(), rpcRequest.getSerializationPolicy(), rpcRequest.getFlags());
			answerer.send("+" + answer);
		} catch (Throwable e) {
			logger.error("Can't Process Request because of thrown Exception at " + service + " with data " + data, e);
			answerer.send("-" + e.getMessage());
			return;
		} finally {
			helper.setThreadLocals(servlet, null);
			// Thread.currentThread().setContextClassLoader(oldclassloader);
		}
	}

}

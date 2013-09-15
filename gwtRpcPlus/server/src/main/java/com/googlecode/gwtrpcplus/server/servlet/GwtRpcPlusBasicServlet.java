package com.googlecode.gwtrpcplus.server.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gwt.user.server.rpc.RPCServletUtils;
import com.google.inject.Inject;
import com.googlecode.gwtrpcplus.server.impl.RpcManagerServer;

/**
 * Bind this to /$modulename$/gwtrpcplus
 */
@Singleton
public class GwtRpcPlusBasicServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Inject
	private RpcManagerServer manager;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if ("true".equals(req.getHeader("longpush")))
			longpush(req, resp);
		else
			request(req, resp);
	}

	protected void longpush(HttpServletRequest request, HttpServletResponse resp) {
		String clientId = request.getHeader("clientId");
		if (clientId == null) {
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		// TODO Move to RpcManager
		ArrayList<String> responses = new ArrayList<String>();

		// WARN, waittime without response have to be > clients timeout
		String r = manager.getResponse(clientId, 25, TimeUnit.SECONDS);
		if (r != null) {
			// We have a response in the queue, so answer it directly
			responses.add(r);
			// Add the other Responses queued
			while ((r = manager.getResponse(clientId)) != null)
				responses.add(r);
		}

		StringBuffer b = new StringBuffer();
		try {
			for (String re : responses) {
				b.append(re);
				b.append("\n");
			}
			String response = b.toString();
			boolean gzipEncode = RPCServletUtils.acceptsGzipEncoding(request)
					&& RPCServletUtils.exceedsUncompressedContentLengthLimit(response);

			RPCServletUtils.writeResponse(getServletContext(), resp, response, gzipEncode);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void request(HttpServletRequest request, HttpServletResponse resp) throws IOException {
		StringBuilder data = new StringBuilder();
		{
			BufferedReader in = request.getReader();
			char[] tmp = new char[100];
			int len = 0;
			do {
				len = in.read(tmp, 0, tmp.length);
				if (len > 0)
					data.append(tmp, 0, len);
			} while (len > 0);
		}
		
		String clientId = request.getHeader("clientId");
		if (clientId == null) {
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		manager.onCall(clientId, data.toString(), request);

		// TODO Make async
		String response = manager.getResponse(clientId);

		if (response != null) {
			boolean gzipEncode = RPCServletUtils.acceptsGzipEncoding(request)
					&& RPCServletUtils.exceedsUncompressedContentLengthLimit(response);

			RPCServletUtils.writeResponse(getServletContext(), resp, response, gzipEncode);
		} else {
			resp.setStatus(HttpServletResponse.SC_OK);
		}
	}
}

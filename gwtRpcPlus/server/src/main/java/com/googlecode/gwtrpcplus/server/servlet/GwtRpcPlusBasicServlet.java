package com.googlecode.gwtrpcplus.server.servlet;

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

	private void longpush(HttpServletRequest request, HttpServletResponse resp) {
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

	private void request(HttpServletRequest request, HttpServletResponse resp) throws IOException {
		StringBuilder finalResponse = new StringBuilder();
		for (String data = request.getReader().readLine(); data != null; data = request.getReader().readLine()) {
			String clientId = request.getHeader("clientId");
			if (clientId == null) {
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}

			// String contextPath = req.getContextPath();
			// String strongname = request.getHeader(RpcRequestBuilder.STRONG_NAME_HEADER);
			// String modulebase = request.getHeader(RpcRequestBuilder.MODULE_BASE_HEADER);
			// manager.onCall(clientId, data, request.getContextPath(), strongname, modulebase);

			manager.onCall(clientId, data, request);

			// TODO Make async
			String response = manager.getResponse(clientId);
			if (response != null) {
				if (finalResponse.length() > 0)
					finalResponse.append('\n');
				finalResponse.append(response);
			}
		}
		// Answer
		String response = finalResponse.toString();
		if (!response.isEmpty()) {
			boolean gzipEncode = RPCServletUtils.acceptsGzipEncoding(request)
					&& RPCServletUtils.exceedsUncompressedContentLengthLimit(response);

			RPCServletUtils.writeResponse(getServletContext(), resp, response, gzipEncode);
		} else {
			resp.setStatus(HttpServletResponse.SC_OK);
		}
	}
}

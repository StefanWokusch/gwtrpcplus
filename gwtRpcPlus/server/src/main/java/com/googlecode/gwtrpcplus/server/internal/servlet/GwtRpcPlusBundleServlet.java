package com.googlecode.gwtrpcplus.server.internal.servlet;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gwt.user.server.rpc.RPCServletUtils;
import com.googlecode.gwtrpcplus.server.internal.RpcManagerServer;

/**
 * Bind this to /$modulename$/gwtRpcPlusBundle
 */
public class GwtRpcPlusBundleServlet extends GwtRpcPlusBasicServlet {
  private static final long serialVersionUID = 1L;

  public GwtRpcPlusBundleServlet(RpcManagerServer manager) {
    super(manager);
  }

  @Override
  protected void request(HttpServletRequest request, HttpServletResponse resp) throws IOException {
    StringBuilder finalResponse = new StringBuilder();
    BufferedReader in = request.getReader();
    char[] tmp = new char[100];

    while (true) {
      String l = in.readLine();
      if (l == null) {
        // No more Requests
        String response = finalResponse.toString();
        if (!response.isEmpty()) {
          boolean gzipEncode = RPCServletUtils.acceptsGzipEncoding(request)
              && RPCServletUtils.exceedsUncompressedContentLengthLimit(response);

          RPCServletUtils.writeResponse(getServletContext(), resp, response, gzipEncode);
        } else {
          resp.setStatus(HttpServletResponse.SC_OK);
        }
        return;
      }
      StringBuilder data = new StringBuilder();
      int read = 0;
      for (int length = Integer.parseInt(l); length > 0;) {
        read = in.read(tmp, 0, Math.min(length, tmp.length));
        if (read > 0) {
          data.append(tmp, 0, read);
          length -= read;
        }
        if (length > 0 && read == -1) {
          throw new RuntimeException("Unexpected End of Stream");
        }
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
        if (finalResponse.length() > 0)
          finalResponse.append('\n');
        finalResponse.append(response);
      }
    }
  }
}

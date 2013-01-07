package de.joe.core.rpc.server.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.google.inject.Inject;

import de.joe.core.rpc.server.RpcManagerServer;

/**
 * Bind to /$modulename$/gwtrpcplus
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

  private void longpush(HttpServletRequest req, HttpServletResponse resp) {
    String clientId = req.getHeader("clientId");
    if (clientId == null) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    ArrayList<String> response = new ArrayList<String>();

    // WARN, waittime without response have to be > clients timeout
    String r = manager.getResponse(clientId, 25, TimeUnit.SECONDS);
    if (r != null) {
      // We have a response in the queue, so answer it directly
      response.add(r);
      // Add the other Responses queued
      while ((r = manager.getResponse(clientId)) != null)
        response.add(r);
    }

    resp.setStatus(HttpServletResponse.SC_OK);
    try {
      for (String re : response) {
        resp.getWriter().write(re);
        resp.getWriter().write("\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void request(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String data = req.getReader().readLine();

    String clientId = req.getHeader("clientId");
    if (clientId == null) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    // String contextPath = req.getContextPath();
    String strongname = req.getHeader(RpcRequestBuilder.STRONG_NAME_HEADER);
    String modulebase = req.getHeader(RpcRequestBuilder.MODULE_BASE_HEADER);

    manager.onCall(clientId, req.getContextPath(), data, strongname, modulebase);
    // TODO Make async
    String response = manager.getResponse(clientId);

    if (response != null) {
      resp.getWriter().write(response);
    }

    resp.setStatus(HttpServletResponse.SC_OK);
  }
}

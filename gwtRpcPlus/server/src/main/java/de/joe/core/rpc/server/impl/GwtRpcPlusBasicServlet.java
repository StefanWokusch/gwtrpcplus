package de.joe.core.rpc.server.impl;

import java.io.IOException;
import java.util.UUID;

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
    String data = req.getReader().readLine();

    String clientId = req.getHeader("clientId");
    if (clientId == null) {
      clientId = UUID.randomUUID().toString();
    }

//    String contextPath = req.getContextPath();
    String strongname = req.getHeader(RpcRequestBuilder.STRONG_NAME_HEADER);
    String modulebase = req.getHeader(RpcRequestBuilder.MODULE_BASE_HEADER);

    manager.onCall(clientId, data, strongname, modulebase);
    // TODO Make async
    String response = manager.getResponse(clientId);

    resp.getWriter().write(response);

    resp.setStatus(HttpServletResponse.SC_OK);
  }
}

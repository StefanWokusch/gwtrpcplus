package de.joe.core.rpc.server.impl;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Singleton;

@Singleton
public class GwtRpcPlusWebsocketDummy extends HttpServlet {
  private static final long serialVersionUID = 1L;

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    resp.getWriter().write("GwtRpcPlus-Websockets are not available for this Server.");
    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }
}

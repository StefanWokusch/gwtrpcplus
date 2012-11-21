package de.joe.core.rpc.server;

import javax.servlet.http.HttpServletRequest;

public interface RequestMethodHandler {
  public interface RequestMethodAnswerer {
    void send(String answer);
  }

  void process(String service, String data, HttpServletRequest request, RequestMethodAnswerer answerer);
}

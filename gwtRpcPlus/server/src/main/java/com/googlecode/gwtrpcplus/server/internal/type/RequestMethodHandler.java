package com.googlecode.gwtrpcplus.server.internal.type;

import javax.servlet.http.HttpServletRequest;

public interface RequestMethodHandler {
  interface RequestMethodAnswerer {
    void send(String answer);
  }

  void process(String service, String data, HttpServletRequest request, RequestMethodAnswerer answerer);

  /**
   * @return Name of the Methodtype, matching with the RequestMethod on the Cient
   */
  String getRequestTypeName();
}

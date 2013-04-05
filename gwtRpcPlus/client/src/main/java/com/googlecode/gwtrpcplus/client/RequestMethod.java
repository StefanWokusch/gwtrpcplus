package com.googlecode.gwtrpcplus.client;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;

/**
 * Handling of a ServiceCall. These Implementations will handle resend, serverpush and so on
 */
public interface RequestMethod {

  public static interface RequestPlus {
    /**
     * @return the rpc-call infos for the Server
     */
    String getRequestString();

    /**
     * @return the full qualified name of the ServiceInterface (delivered by the Async_Proxy)
     */
    String getServiceName();

    /**
     * Called when a response arrived for a Request
     * 
     * @param answer answer from the server
     */
    void onAnswer(String answer);

    /**
     * Called when no Answer arrived from the server.
     * 
     * @return true when the Request should be resend
     */
    boolean onTimeout();

    /**
     * @return a unique name for the RequestMethod, matching to some RequestMethodHandler on the Serverside
     */
    String getRequestTypeName();
  }

  public static interface ConnectionHandler {
    /**
     * Adds a Request and send it to the Server
     * 
     * @param request Request to send
     */
    void addRequest(RequestPlus request);

    /**
     * Removes a Request from the list.
     * 
     * You need to call this when you don't expect any more answer.
     * 
     * For FireAndForget just call addRequest(r);removeRequest(r);
     * 
     * @param request request to remove
     */
    void removeRequest(RequestPlus request);
  }

  void setHandler(ConnectionHandler handler);


  /**
   * Called when the Async Method is invoked
   * 
   * @param requestData Data to send
   * @param requestCallback callback to call (use RequestHelper for easy usage)
   * @return Request to cancle the request (if supported)
   */
  Request call(String requestData, RequestCallback requestCallback);
}

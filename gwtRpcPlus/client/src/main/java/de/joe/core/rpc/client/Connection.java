package de.joe.core.rpc.client;

/**
 * Connection to the Server (example: websocket, Longpolling,...)
 */
public interface Connection {
  public static interface RecieveHandler {
    /**
     * Called when the Server responds some answers
     * 
     * @param answer answer of the Server
     */
    void onRecieve(String answer);

    /**
     * Called when the Connection has been established (after calling connect())
     */
    void onConnected();

    /**
     * Called when the Connection is closed (like serverDown, websocket-closed,...)
     */
    void onDisconnect();
  }

  /**
   * Sets the RecieveHandler
   * 
   * @param handler
   */
  void setHandler(RecieveHandler handler);

  /**
   * Called when the Connection should be established
   */
  void connect();

  /**
   * Called when the Connection should be canceled
   */
  void disconnect();

  /**
   * Called to send a message to the Server
   * 
   * @param request request to send
   */
  void send(String request);

  /**
   * Called when responses are expected. For example at serverpush-requests waiting for more
   * Responses.
   * 
   * This is an indicator for example for Longpolling, to keep a connection to the Server.
   * 
   * @param pending true when responses are expected
   */
  void setPending(boolean pending);
}

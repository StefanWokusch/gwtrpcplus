package com.googlecode.gwtrpcplus.server.impl;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.gwt.event.shared.HandlerRegistration;
import com.googlecode.gwtrpcplus.server.util.Logger;


/**
 * A connected Client to the Server
 */
public class RpcPlusClient {
  private Logger logger = new Logger(RpcPlusClient.class);

  private final Set<RpcPlusClientHandler> handlers = Collections.newSetFromMap(new ConcurrentHashMap<RpcPlusClientHandler, Boolean>());

  private final BlockingQueue<String> responses = new LinkedBlockingQueue<String>();

  private boolean disconnected = false;

  private long lastCall = System.currentTimeMillis();

  /**
   * Handler for Answers
   */
  public static interface RpcPlusClientHandler {
    /**
     * Called when a answer was created for a client
     * 
     * @param answer the Answer to send
     * @return true when the answer could be send, false for example on ws-disconnect
     */
    boolean onAnswer(String answer);
  }

  public HandlerRegistration addHandler(final RpcPlusClientHandler handler) {
    handlers.add(handler);
    return new HandlerRegistration() {
      @Override
      public void removeHandler() {
        handlers.remove(handler);
      }
    };
  }

  /**
   * @return true when the Client isn't called the last time and can be cleaned
   */
  public boolean isObsolete() {
    // TODO Remove this check
    // Don't remove when an active websocketconnection is established
    if (!handlers.isEmpty())
      return false;

    return disconnected || (System.currentTimeMillis() - lastCall) > TimeUnit.MINUTES.toMillis(1);
  }

  /**
   * Mark the Client as inUse
   */
  public void touch() {
    lastCall = System.currentTimeMillis();
  }

  public void addResponse(String response) {
    if (disconnected) {
      logger.trace("Ignoring Response because of client disconnected");
      return;
    }
    for (RpcPlusClientHandler h : handlers) {
    	if (h.onAnswer(response)) {
        touch();
        return;
      }
    }

    responses.add(response);
  }

  public String getResponse() {
    touch();
    return responses.poll();
  }

  public String getResponse(long timeout, TimeUnit unit) {
    try {
      return responses.poll(timeout, unit);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    } finally {
      touch();
    }
  }

  public void disconnect() {
    if (!disconnected) {
      disconnected = true;
      responses.clear();

      // TODO Inform Serverpush-Methods

      // TODO Inform Handlers
    }
  }


}

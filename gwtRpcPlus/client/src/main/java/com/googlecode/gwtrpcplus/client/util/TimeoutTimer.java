package com.googlecode.gwtrpcplus.client.util;

import com.google.gwt.user.client.Timer;
import com.googlecode.gwtrpcplus.server.internal.RpcPlusClient;


public interface TimeoutTimer {

  abstract class DefaultTimer implements TimeoutTimer {
    /**
     * ms to fire a timeout
     * 
     * WARN: have to equals {@link RpcPlusClient}s KEEPALIVE_TIMEOUT
     */
    public static final int TIMEOUT = 30000;

    private Timer timer = new Timer() {
      @Override
      public void run() {
        scheduled = false;
        stoppable = true;
        fire();
      }
    };

    public DefaultTimer() {
    }

    private boolean stoppable = true;
    private boolean scheduled = false;

    public void schedule(boolean stoppable) {
      this.stoppable = this.stoppable && stoppable;
      if (this.stoppable || !scheduled) {
        scheduled = true;
        timer.schedule(TIMEOUT);
      }
    }
  }

  /**
   * Schedules the timer
   * 
   * @param stoppable true when a other schedule call can stop the old one
   */
  void schedule(boolean stoppable);

  void fire();
}

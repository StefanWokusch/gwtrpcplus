package de.joe.core.rpc.client.util;

import com.google.gwt.user.client.Timer;

import de.joe.core.rpc.server.impl.GwtRpcPlusBasicServlet;

public interface MyTimer {

  public static abstract class DefaultTimer implements MyTimer {
    /**
     * ms to fire a timeout
     * 
     * WARN: {@link GwtRpcPlusBasicServlet}s timeout-check
     */
    private static final int TIMEOUT = 30000;

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

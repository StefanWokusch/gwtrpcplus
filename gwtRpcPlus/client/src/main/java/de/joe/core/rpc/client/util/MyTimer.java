package de.joe.core.rpc.client.util;

import com.google.gwt.user.client.Timer;

public interface MyTimer {

  public static abstract class DefaultTimer implements MyTimer {
    /**
     * ms to fire a timeout
     */
    private static final int TIMEOUT = 5000;

    private Timer timer = new Timer() {
      @Override
      public void run() {
        fire();
      }
    };

    public DefaultTimer() {
    }

    public void schedule() {
      timer.schedule(TIMEOUT);
    }
  }

  void schedule();

  void fire();
}

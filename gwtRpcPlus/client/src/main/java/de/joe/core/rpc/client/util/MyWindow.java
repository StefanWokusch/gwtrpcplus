package de.joe.core.rpc.client.util;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingHandler;


public interface MyWindow {

  public static class DefaultWindow implements MyWindow {

    public DefaultWindow() {
    }

    @Override
    public void addWindowClosingHandler(ClosingHandler handler) {
      Window.addWindowClosingHandler(handler);
    }
  }

  void addWindowClosingHandler(ClosingHandler closingHandler);
}

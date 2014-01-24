package com.googlecode.gwtrpcplus.server.internal.util;

public class Logger {

  private final Class<?> clazz;

  public Logger(Class<?> clazz) {
    this.clazz = clazz;
  }

  private static boolean logged = false;


  public void error(String text, Object... objects) {
    try {
      org.slf4j.LoggerFactory.getLogger(clazz).error(text, objects);
    } catch (NoClassDefFoundError ex) {
      if (!logged) {
        logged = true;
        System.out.println("No slf4j found. No Logging available for GwtRpcPlus");
      }
    }
  }

  public void error(String text, Throwable e) {
    try {
      org.slf4j.LoggerFactory.getLogger(clazz).error(text, e);
    } catch (NoClassDefFoundError ex) {
      if (!logged) {
        logged = true;
        System.out.println("No slf4j found. No Logging available for GwtRpcPlus");
      }
    }
  }

  public void warn(String text, Object... objects) {
    try {
      org.slf4j.LoggerFactory.getLogger(clazz).warn(text, objects);
    } catch (NoClassDefFoundError e) {
      if (!logged) {
        logged = true;
        System.out.println("No slf4j found. No Logging available for GwtRpcPlus");
      }
    }
  }

  public void info(String text, Object... objects) {
    try {
      org.slf4j.LoggerFactory.getLogger(clazz).info(text, objects);
    } catch (NoClassDefFoundError e) {
      if (!logged) {
        logged = true;
        System.out.println("No slf4j found. No Logging available for GwtRpcPlus");
      }
    }
  }

  public void debug(String text, Object... objects) {
    try {
      org.slf4j.LoggerFactory.getLogger(clazz).debug(text, objects);
    } catch (NoClassDefFoundError e) {
      if (!logged) {
        logged = true;
        System.out.println("No slf4j found. No Logging available for GwtRpcPlus");
      }
    }
  }

  public void trace(String text, Object... objects) {
    try {
      org.slf4j.LoggerFactory.getLogger(clazz).trace(text, objects);
    } catch (NoClassDefFoundError e) {
      if (!logged) {
        logged = true;
        System.out.println("No slf4j found. No Logging available for GwtRpcPlus");
      }
    }
  }


}

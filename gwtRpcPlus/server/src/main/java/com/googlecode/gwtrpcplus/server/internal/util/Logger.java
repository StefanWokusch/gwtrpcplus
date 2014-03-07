package com.googlecode.gwtrpcplus.server.internal.util;

public class Logger {

  private final Class<?> clazz;

  public Logger(Class<?> clazz) {
    this.clazz = clazz;
  }

  private static boolean noLoggerDetected = false;

  private org.slf4j.Logger logger;

  protected org.slf4j.Logger getLogger() {
    if (noLoggerDetected || logger != null)
      return logger;
    try {
      return org.slf4j.LoggerFactory.getLogger(clazz);
    } catch (NoClassDefFoundError ex) {
      if (!noLoggerDetected) {
        noLoggerDetected = true;
        System.out.println("No slf4j found. No Logging available for GwtRpcPlus.");
      }
    }
    return null;
  }

  public void error(String text, Object... objects) {
    org.slf4j.Logger logger = getLogger();
    if (logger != null)
      logger.error(text, objects);

  }

  public void error(String text, Throwable e) {
    org.slf4j.Logger logger = getLogger();
    if (logger != null)
      logger.error(text, e);
  }

  public void warn(String text, Object... objects) {
    org.slf4j.Logger logger = getLogger();
    if (logger != null)
      logger.warn(text, objects);
  }

  public void info(String text, Object... objects) {
    org.slf4j.Logger logger = getLogger();
    if (logger != null)
      logger.info(text, objects);
  }

  public void debug(String text, Object... objects) {
    org.slf4j.Logger logger = getLogger();
    if (logger != null)
      logger.debug(text, objects);
  }

  public void trace(String text, Object... objects) {
    org.slf4j.Logger logger = getLogger();
    if (logger != null)
      logger.trace(text, objects);
  }

  public boolean isTraceEnabled() {
    org.slf4j.Logger logger = getLogger();
    if (logger != null)
      return logger != null && logger.isTraceEnabled();
    return false;
  }

  public boolean isDebugEnabled() {
    org.slf4j.Logger logger = getLogger();
    if (logger != null)
      return logger != null && logger.isDebugEnabled();
    return false;
  }
}

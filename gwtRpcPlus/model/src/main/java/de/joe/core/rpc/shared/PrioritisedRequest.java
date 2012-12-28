package de.joe.core.rpc.shared;

import com.google.gwt.http.client.Request;

public abstract class PrioritisedRequest extends Request {
  /**
   * Priority lower is more important
   * 
   * @param priority
   */
  public abstract void setPriority(double priority);
}
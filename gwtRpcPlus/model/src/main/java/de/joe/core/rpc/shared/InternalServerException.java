package de.joe.core.rpc.shared;

import com.google.gwt.user.client.rpc.StatusCodeException;

public class InternalServerException extends StatusCodeException {
  private static final long serialVersionUID = 1L;

  /**
   * @param cause a simple Text whats the problem.
   */
  public InternalServerException(String cause) {
    super(500, "An Interal Serverexception was thrown (\"" + cause + "\"). Look at the Serverlog for more details.");
  }
}

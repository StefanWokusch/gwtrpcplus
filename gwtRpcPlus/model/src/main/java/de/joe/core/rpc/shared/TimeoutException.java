package de.joe.core.rpc.shared;

import com.google.gwt.user.client.rpc.StatusCodeException;

public class TimeoutException extends StatusCodeException {
  private static final long serialVersionUID = 1L;
  private boolean resendAllowed;

  public TimeoutException(boolean resendAllowed) {
    super(408, "The Request timed out.");
    this.resendAllowed = resendAllowed;
  }

  /**
   * @return true when the Request was automaticly resended
   */
  public boolean isResendAllowed() {
    return resendAllowed;
  }
}

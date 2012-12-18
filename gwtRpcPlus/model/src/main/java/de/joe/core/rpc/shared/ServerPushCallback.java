package de.joe.core.rpc.shared;

import com.google.gwt.user.client.rpc.AsyncCallback;

public abstract class ServerPushCallback<T> implements AsyncCallback<T> {
  /*
   * Hack to make the special ServerPushCallback simple
   */
  public static boolean nextIsFinished;

  @Override
  public final void onSuccess(T result) {
    if (nextIsFinished)
      onFinish(result);
    else
      onAnswer(result);
  }

  @Override
  public final void onFailure(Throwable caught) {
    if (caught instanceof TimeoutException && ((TimeoutException) caught).isResendAllowed()) {
      onResend();
    } else {
      onException(caught);
    }
  }

  /**
   * Called when the Request was resended by the Client (Connection to Server lost)
   * 
   * Needed a @ResendAllowed Annotation above the called serviceMethod
   */
  protected void onResend() {
  }

  /**
   * Simple Answer, more of them are expected
   * 
   * @param result
   */
  public abstract void onAnswer(T result);

  /**
   * The last answer to the Request
   * 
   * Default:: call onAnswer(result)
   * 
   * @param result
   */
  public void onFinish(T result) {
    onAnswer(result);
  }

  /**
   * Called when the Request fails
   * 
   * @param e thrown Exception
   */
  public abstract void onException(Throwable e);
}

package com.googlecode.gwtrpcplus.shared;

public interface ReturnHandler<Type> {
  /**
   * Use this, when you want to send more answers
   * 
   * @param obj answer to send
   */
  void answer(Type obj);

  /**
   * Use this for the last answer. The Serverpush-connectin will be stopped.
   * 
   * @param obj answer to send
   */
  void finish(Type obj);

  /**
   * Use this when u want to abort because of a exception (like in the interface)
   * 
   * @param caught the Exception to throw
   */
  void abort(Exception caught);
}

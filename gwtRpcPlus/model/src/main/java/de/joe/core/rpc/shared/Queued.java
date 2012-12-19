package de.joe.core.rpc.shared;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Queued {
  /**
   * Amount of pending Requests of the Method
   */
  int value() default 1;
}

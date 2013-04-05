package com.googlecode.gwtrpcplus.client;

import java.util.List;

public interface ConnectionProvider {

  /**
   * @return available connections (already prioritized, first = highest priority)
   */
  List<Connection> get();

}
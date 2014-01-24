package com.googlecode.gwtrpcplus.server.internal.util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class SimpleServletConfig implements ServletConfig {
  private final Map<String, String> initParams = new HashMap<String, String>();

  private final String servletName;
  private final ServletContext servletContext;


  public SimpleServletConfig(String servletName, ServletContext servletContext) {
    this.servletName = servletName;
    this.servletContext = servletContext;
  }

  @Override
  public String getServletName() {
    return servletName;
  }

  @Override
  public ServletContext getServletContext() {
    return servletContext;
  }

  public String getInitParameter(String s) {
    return initParams.get(s);
  }

  @SuppressWarnings("rawtypes")
  public Enumeration getInitParameterNames() {
    final Iterator<String> iterator = initParams.keySet().iterator();
    return new Enumeration() {
      @Override
      public boolean hasMoreElements() {
        return iterator.hasNext();
      }

      @Override
      public Object nextElement() {
        return iterator.next();
      }
    };
  }
}
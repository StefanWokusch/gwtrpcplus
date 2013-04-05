package com.googlecode.gwtrpcplus.server.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.google.gwt.user.client.rpc.RpcRequestBuilder;

/**
 * Minimal Version of a ServletRequest. Used for example for Websocket-support
 */
public class HttpServletRequestMinimum implements HttpServletRequest {
  private final String permStrongName;
  private final String reqModuleBasePath;
  private final String contextPath;

  public HttpServletRequestMinimum(String contextPath, String permStrongName, String reqModuleBasePath) {
    this.contextPath = contextPath;
    this.permStrongName = permStrongName;
    this.reqModuleBasePath = reqModuleBasePath;
  }

  @Override
  public String getHeader(final String name) {
    if (name.equals(RpcRequestBuilder.STRONG_NAME_HEADER))
      return permStrongName;
    if (name.equals(RpcRequestBuilder.MODULE_BASE_HEADER))
      return reqModuleBasePath;
    throw new RuntimeException("Header \"" + name + "\" not supported");
  }

  @Override
  public String getContextPath() {
    return contextPath;
  }

  @Override
  public Object getAttribute(String name) {
    throw new RuntimeException("Not supported");
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Enumeration getAttributeNames() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getCharacterEncoding() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
    throw new RuntimeException("Not supported");

  }

  @Override
  public int getContentLength() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getContentType() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getParameter(String name) {
    throw new RuntimeException("Not supported");

  }

  @SuppressWarnings("rawtypes")
  @Override
  public Enumeration getParameterNames() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String[] getParameterValues(String name) {
    throw new RuntimeException("Not supported");

  }

  @SuppressWarnings("rawtypes")
  @Override
  public Map getParameterMap() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getProtocol() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getScheme() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getServerName() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public int getServerPort() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public BufferedReader getReader() throws IOException {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getRemoteAddr() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getRemoteHost() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public void setAttribute(String name, Object o) {
    throw new RuntimeException("Not supported");

  }

  @Override
  public void removeAttribute(String name) {
    throw new RuntimeException("Not supported");

  }

  @Override
  public Locale getLocale() {
    throw new RuntimeException("Not supported");

  }

  @SuppressWarnings("rawtypes")
  @Override
  public Enumeration getLocales() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public boolean isSecure() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public RequestDispatcher getRequestDispatcher(String path) {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getRealPath(String path) {
    throw new RuntimeException("Not supported");

  }

  @Override
  public int getRemotePort() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getLocalName() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getLocalAddr() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public int getLocalPort() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getAuthType() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public Cookie[] getCookies() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public long getDateHeader(String name) {
    throw new RuntimeException("Not supported");

  }

  @SuppressWarnings("rawtypes")
  @Override
  public Enumeration getHeaders(String name) {
    throw new RuntimeException("Not supported");

  }

  @SuppressWarnings("rawtypes")
  @Override
  public Enumeration getHeaderNames() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public int getIntHeader(String name) {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getMethod() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getPathInfo() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getPathTranslated() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getQueryString() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getRemoteUser() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public boolean isUserInRole(String role) {
    throw new RuntimeException("Not supported");

  }

  @Override
  public Principal getUserPrincipal() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getRequestedSessionId() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getRequestURI() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public StringBuffer getRequestURL() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public String getServletPath() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public HttpSession getSession(boolean create) {
    throw new RuntimeException("Not supported");

  }

  @Override
  public HttpSession getSession() {
    throw new RuntimeException("Not supported");

  }

  @Override
  public boolean isRequestedSessionIdValid() {
    throw new RuntimeException("Not supported");
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {
    throw new RuntimeException("Not supported");
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {
    throw new RuntimeException("Not supported");
  }

  @Override
  public boolean isRequestedSessionIdFromUrl() {
    throw new RuntimeException("Not supported");
  }
}

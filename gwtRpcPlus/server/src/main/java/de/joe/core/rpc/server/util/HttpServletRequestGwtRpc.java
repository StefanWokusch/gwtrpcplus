package de.joe.core.rpc.server.util;

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

public class HttpServletRequestGwtRpc implements HttpServletRequest {
  private final String permStrongName;
  private final String reqModuleBasePath;
  private final String contextPath;

  public HttpServletRequestGwtRpc(String contextPath, String permStrongName, String reqModuleBasePath) {
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
    // return super.getHeader(name);
    return null;
  }

  @Override
  public String getContextPath() {
    return contextPath;
  }

  @Override
  public Object getAttribute(String name) {
    assert(false):"Not supported";
    return null;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Enumeration getAttributeNames() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public String getCharacterEncoding() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
    assert(false):"Not supported";

  }

  @Override
  public int getContentLength() {
    assert(false):"Not supported";
    return 0;
  }

  @Override
  public String getContentType() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public String getParameter(String name) {
    assert(false):"Not supported";
    return null;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Enumeration getParameterNames() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public String[] getParameterValues(String name) {
    assert(false):"Not supported";
    return null;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Map getParameterMap() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public String getProtocol() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public String getScheme() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public String getServerName() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public int getServerPort() {
    assert(false):"Not supported";
    return 0;
  }

  @Override
  public BufferedReader getReader() throws IOException {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public String getRemoteAddr() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public String getRemoteHost() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public void setAttribute(String name, Object o) {
    assert(false):"Not supported";

  }

  @Override
  public void removeAttribute(String name) {
    assert(false):"Not supported";

  }

  @Override
  public Locale getLocale() {
    assert(false):"Not supported";
    return null;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Enumeration getLocales() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public boolean isSecure() {
    assert(false):"Not supported";
    return false;
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String path) {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public String getRealPath(String path) {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public int getRemotePort() {
    assert(false):"Not supported";
    return 0;
  }

  @Override
  public String getLocalName() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public String getLocalAddr() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public int getLocalPort() {
    assert(false):"Not supported";
    return 0;
  }

  @Override
  public String getAuthType() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public Cookie[] getCookies() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public long getDateHeader(String name) {
    assert(false):"Not supported";
    return 0;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Enumeration getHeaders(String name) {
    assert(false):"Not supported";
    return null;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Enumeration getHeaderNames() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public int getIntHeader(String name) {
    assert(false):"Not supported";
    return 0;
  }

  @Override
  public String getMethod() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public String getPathInfo() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public String getPathTranslated() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public String getQueryString() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public String getRemoteUser() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public boolean isUserInRole(String role) {
    assert(false):"Not supported";
    return false;
  }

  @Override
  public Principal getUserPrincipal() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public String getRequestedSessionId() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public String getRequestURI() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public StringBuffer getRequestURL() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public String getServletPath() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public HttpSession getSession(boolean create) {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public HttpSession getSession() {
    assert(false):"Not supported";
    return null;
  }

  @Override
  public boolean isRequestedSessionIdValid() {
    assert(false):"Not supported";
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {
    assert(false):"Not supported";
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {
    assert(false):"Not supported";
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromUrl() {
    assert(false):"Not supported";
    return false;
  }
}

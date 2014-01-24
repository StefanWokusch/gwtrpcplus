package com.googlecode.gwtrpcplus.server.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.google.gwt.event.shared.HandlerRegistration;
import com.googlecode.gwtrpcplus.server.internal.RpcPlusClient.RpcPlusClientHandler;
import com.googlecode.gwtrpcplus.server.internal.type.RequestMethodHandler;
import com.googlecode.gwtrpcplus.server.internal.type.RequestMethodHandler.RequestMethodAnswerer;
import com.googlecode.gwtrpcplus.server.internal.type.RequestMethodHandlerBasic;
import com.googlecode.gwtrpcplus.server.internal.type.RequestMethodHandlerQueued;
import com.googlecode.gwtrpcplus.server.internal.type.RequestMethodHandlerServerpush;
import com.googlecode.gwtrpcplus.server.internal.util.HttpServletRequestMinimum;
import com.googlecode.gwtrpcplus.server.internal.util.Logger;

public class RpcManagerServer {
  private Logger logger = new Logger(RpcManagerServer.class);

  private final HashMap<String, RequestMethodHandler> requestMethodHandlers;

  private final ConcurrentHashMap<String, RpcPlusClient> clients = new ConcurrentHashMap<String, RpcPlusClient>();

  public RpcManagerServer(RequestMethodHandlerBasic basic, RequestMethodHandlerServerpush push,
      RequestMethodHandlerQueued queued) {
    // TODO inject the executor
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    // TODO make configurable
    this.requestMethodHandlers = new HashMap<>();
    this.requestMethodHandlers.put(basic.getRequestTypeName(), basic);
    this.requestMethodHandlers.put(push.getRequestTypeName(), push);
    this.requestMethodHandlers.put(queued.getRequestTypeName(), queued);
    executor.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        Iterator<Entry<String, RpcPlusClient>> it = clients.entrySet().iterator();
        while (it.hasNext()) {
          Entry<String, RpcPlusClient> e = it.next();
          if (e.getValue().isObsolete()) {
            logger.info("Client timeouted: id={}", e.getKey());
            e.getValue().disconnect();
            it.remove();
          }
        }
        logger.trace("Connected Clients: {}", clients.size());
      }
    }, 0, 30, TimeUnit.SECONDS);
  }

  public void onCall(final String clientId, String data, String contextPath, String permStrongName,
      String reqModuleBasePath, HttpSession httpSession) {
    if (contextPath == null)
      logger.warn("No ContextPath given");

    onCall(clientId, data, new HttpServletRequestMinimum(contextPath, permStrongName, reqModuleBasePath, httpSession));
  }

  public void onCall(final String clientId, String data, HttpServletRequest req) {
    logger.trace("Call from Client {}", data.substring(0, Math.min(data.length(), 150)));

    if (data.equals("disconnect")) {
      RpcPlusClient client = clients.remove(clientId);
      if (client != null) {
        client.disconnect();
        logger.info("Client timeouted: id={}", clientId);
      }
      return;
    }

    get(clientId).touch();

    final String requestId = data.substring(0, data.indexOf("#"));
    data = data.substring(data.indexOf("#") + 1);
    final String type = data.substring(0, data.indexOf("#"));
    data = data.substring(data.indexOf("#") + 1);
    final String service = data.substring(0, data.indexOf("#"));
    data = data.substring(data.indexOf("#") + 1);

    RequestMethodHandler h = requestMethodHandlers.get(type);
    if (h == null)
      throw new IllegalArgumentException("Type \"" + type + "\" isn't registered");
    h.process(service, data, req, new RequestMethodAnswerer() {
      @Override
      public void send(String answer) {
        String response = requestId + "#" + answer;
        answer(clientId, response);
      }
    });
  }

  private RpcPlusClient get(String clientId) {
    RpcPlusClient client = clients.get(clientId);
    if (client == null) {
      client = new RpcPlusClient();
      RpcPlusClient old = clients.putIfAbsent(clientId, client);
      if (old != null)
        return old;
      logger.info("Client added: id={}", clientId);
    }
    return client;
  }

  private void answer(final String clientId, String response) {
    get(clientId).addResponse(response);
  }

  public HandlerRegistration addHandler(final String clientId, final RpcPlusClientHandler handler) {
    return get(clientId).addHandler(handler);
  }

  public String getResponse(String clientId) {
    return get(clientId).getResponse();
  }

  public String getResponse(String clientId, long timeout, TimeUnit unit) {
    return get(clientId).getResponse(timeout, unit);
  }
}

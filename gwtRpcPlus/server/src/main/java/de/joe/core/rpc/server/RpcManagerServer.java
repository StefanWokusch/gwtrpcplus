package de.joe.core.rpc.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import com.google.gwt.event.shared.HandlerRegistration;

import de.joe.core.rpc.server.RequestMethodHandler.RequestMethodAnswerer;
import de.joe.core.rpc.server.RpcPlusClient.RpcPlusClientHandler;
import de.joe.core.rpc.server.util.HttpServletRequestMinimum;
import de.joe.core.rpc.server.util.Logger;

@Singleton
public class RpcManagerServer {
  private Logger logger = new Logger(RpcManagerServer.class);

  private final HashMap<String, RequestMethodHandler> requestMethodHandlers;

  private final ConcurrentHashMap<String, RpcPlusClient> clients = new ConcurrentHashMap<String, RpcPlusClient>();

  @Inject
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
      String reqModuleBasePath) {
    onCall(clientId, data, new HttpServletRequestMinimum(contextPath, permStrongName, reqModuleBasePath));
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

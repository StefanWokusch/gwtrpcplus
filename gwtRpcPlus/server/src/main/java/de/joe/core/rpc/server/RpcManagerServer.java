package de.joe.core.rpc.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;

import de.joe.core.rpc.server.RequestMethodHandler.RequestMethodAnswerer;
import de.joe.core.rpc.server.util.HttpServletRequestGwtRpc;

public class RpcManagerServer {
  private final HashMap<String, RequestMethodHandler> requestMethodHandlers;

  // TODO Timeout of clients and queues
  private final ConcurrentHashMap<String, BlockingQueue<String>> answers = new ConcurrentHashMap<String, BlockingQueue<String>>();
  private final ConcurrentHashMap<String, Set<AnswerHandler>> handlers = new ConcurrentHashMap<String, Set<AnswerHandler>>();

  /**
   * Handler for Answers
   */
  public static interface AnswerHandler {
    /**
     * Called when a answer was created for a client
     * 
     * @param answer the Answer to send
     * @return true when the answer could be send, false for example on ws-disconnect
     */
    boolean onAnswer(String answer);
  }

  @Inject
  public RpcManagerServer(RequestMethodHandlerBasic basic, RequestMethodHandlerServerpush push) {
    // TODO Configuratble
    this.requestMethodHandlers = new HashMap<>();
    this.requestMethodHandlers.put(basic.getRequestTypeName(), basic);
    this.requestMethodHandlers.put(push.getRequestTypeName(), push);
  }

  public void onCall(final String clientId, String data, String permStrongName, String reqModuleBasePath) {
    onCall(clientId, data, new HttpServletRequestGwtRpc("", permStrongName, reqModuleBasePath));
  }

  public void onCall(final String clientId, String data, HttpServletRequest req) {
    final String id = data.substring(0, data.indexOf("#"));
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
        String response = id + "#" + answer;
        answer(clientId, response);
      }
    });
  }

  private void answer(final String clientId, String response) {
    Set<AnswerHandler> set = handlers.get(clientId);
    if (set != null)
      for (AnswerHandler h : set) {
        if (h.onAnswer(response))
          return;
      }


    BlockingQueue<String> a = answers.get(clientId);
    if (a == null) {
      a = new LinkedBlockingQueue<String>();
      BlockingQueue<String> a2 = answers.putIfAbsent(clientId, a);
      a = a2 != null ? a2 : a;
    }
    a.add(response);
  }

  public HandlerRegistration addHandler(final String clientId, final AnswerHandler handler) {
    Set<AnswerHandler> a = handlers.get(clientId);
    if (a == null) {
      a = Collections.newSetFromMap(new ConcurrentHashMap<AnswerHandler, Boolean>());
      Set<AnswerHandler> a2 = handlers.putIfAbsent(clientId, a);
      a = a2 != null ? a2 : a;
    }
    a.add(handler);
    return new HandlerRegistration() {
      @Override
      public void removeHandler() {
        Set<AnswerHandler> h = handlers.get(clientId);
        h.remove(handler);
      }
    };
  }

  public String getResponse(String clientId) {
    BlockingQueue<String> a = answers.get(clientId);
    try {
      if (a != null)
        return a.poll();
      return null;
    } finally {
      // TODO Nicht mehr benötigte Queues aufräumen
      // if (a.isEmpty()) {
      // if (answers.remove(clientId, a)) {
      // // Check for bad removing
      // if (!a.isEmpty()) {
      // BlockingQueue<String> a2 = answers.putIfAbsent(clientId, new BlockingArrayQueue<String>());
      // }
      // }
      // }
    }
  }

  public String getResponse(String clientId, long timeout, TimeUnit unit) {
    BlockingQueue<String> a = answers.get(clientId);
    try {
      if (a != null)
        return a.poll(timeout, unit);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      // TODO Nicht mehr benötigte Queues aufräumen
      // if (a.isEmpty()) {
      // if (answers.remove(clientId, a)) {
      // // Check for bad removing
      // if (!a.isEmpty()) {
      // BlockingQueue<String> a2 = answers.putIfAbsent(clientId, new BlockingArrayQueue<String>());
      // }
      // }
      // }
    }
    return null;
  }
}

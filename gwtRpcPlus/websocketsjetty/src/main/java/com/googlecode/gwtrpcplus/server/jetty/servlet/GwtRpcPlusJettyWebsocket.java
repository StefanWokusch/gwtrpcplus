package com.googlecode.gwtrpcplus.server.jetty.servlet;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Singleton;
import com.googlecode.gwtrpcplus.server.impl.RpcManagerServer;
import com.googlecode.gwtrpcplus.server.impl.RpcPlusClient.RpcPlusClientHandler;
import com.googlecode.gwtrpcplus.server.util.Logger;

@Singleton
public class GwtRpcPlusJettyWebsocket extends WebSocketServlet {
	private static final long serialVersionUID = 1L;

	private final static Logger logger = new Logger(GwtRpcPlusJettyWebsocket.class);

	@Inject
	private Provider<GwtRpcSocket> provider;

	private final ThreadLocal<HttpServletRequest> currentRequest = new ThreadLocal<>();

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		currentRequest.set(request);
		try {
			super.service(request, response);
		} finally {
			currentRequest.remove();
		}
	}

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.register(GwtRpcPlusJettyWebsocket.class);
		factory.setCreator(new WebSocketCreator() {
			@Override
			public Object createWebSocket(UpgradeRequest req, UpgradeResponse resp) {
				return provider.get().init(currentRequest.get().getContextPath(), currentRequest.get().getSession());
			}
		});
	}

	@WebSocket
	public static class GwtRpcSocket {
		// private HttpServletRequestGwtRpc request;
		private String clientId;
		private String permutationStrongName;
		private String moduleBasePath;
		private Session session;
		private HttpSession httpSession;
		// private ReadWriteLock lock = new ReentrantReadWriteLock();
		private final RpcManagerServer manager;
		private HandlerRegistration handlerReg;
		private String contextPath;

		@Inject
		public GwtRpcSocket(/* @ShortRunningTasks */ExecutorService executor, RpcManagerServer manager) {
			this.manager = manager;
		}

		public GwtRpcSocket init(String contextPath, HttpSession httpSession) {
			this.contextPath = contextPath;
			this.httpSession = httpSession;
			return this;
		}

		// @Override
		@OnWebSocketConnect
		public void onOpen(Session session) {
			logger.info("Client connected: {}", session);
			this.session = session;
		}

		private boolean isInit = false;

		@OnWebSocketMessage
		public void onMessage(final String data) {
			logger.trace("Data recieved: {}", data);
			if (!isInit) {
				isInit = true;
				processInit(data);
			} else {
				manager.onCall(clientId, data, contextPath, permutationStrongName, moduleBasePath, httpSession);
			}
		}

		// private boolean isConnected = true;

		@OnWebSocketClose
		public void onClose(int statusCode, String reason) {
			// isConnected = false;
			logger.info("Client disconnected {}: {} (code: {})", session, reason, statusCode);
			if (handlerReg != null)
				handlerReg.removeHandler();
		}

		private void processInit(String data) {
			clientId = data.substring(0, data.indexOf("#"));
			permutationStrongName = data.substring(0, data.indexOf("#"));
			moduleBasePath = data.substring(data.indexOf("#") + 1);
			logger.debug("Client initialized with PermutationStrongName: \"{} \" modulBasePath:\"{}\"",
					permutationStrongName, moduleBasePath);
			handlerReg = manager.addHandler(clientId, new RpcPlusClientHandler() {
				@Override
				public boolean onAnswer(String answer) {
					answer = answer + "\n";
					if (session.isOpen()) {
						try {
							logger.trace("send: {}", answer);
							session.getRemote().sendString(answer);
							return true;
						} catch (Throwable e) {
							// TODO: handle exception
							logger.error("Exception while Sending Message. This could caused by disconnecting.", e);
						}
					}
					return false;
				}
			});
		}
	}
}

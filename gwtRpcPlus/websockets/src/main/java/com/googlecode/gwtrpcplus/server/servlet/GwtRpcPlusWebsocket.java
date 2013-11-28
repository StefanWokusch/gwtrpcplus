package com.googlecode.gwtrpcplus.server.servlet;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;

import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.googlecode.gwtrpcplus.server.impl.RpcManagerServer;
import com.googlecode.gwtrpcplus.server.impl.RpcPlusClient.RpcPlusClientHandler;
import com.googlecode.gwtrpcplus.server.util.Logger;

//@Singleton
public class GwtRpcPlusWebsocket extends Endpoint {
	// private static final long serialVersionUID = 1L;

	private final static Logger logger = new Logger(GwtRpcPlusWebsocket.class);

	@Inject
	private Provider<GwtRpcSocket> provider;

	// private final ThreadLocal<HttpServletRequest> currentRequest = new ThreadLocal<>();

	private final HashMap<String, GwtRpcSocket> openSockets = new HashMap<>();

	@Override
	public void onOpen(Session session, EndpointConfig config) {
		// Injector injector = (Injector) config.getUserProperties().get(Injector.class.getName());
		// GwtRpcSocket socket = injector.getInstance(GwtRpcSocket.class);
		HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
		GwtRpcSocket socket = provider.get();
		socket.init(session, httpSession);
		session.addMessageHandler(socket);
		openSockets.put(session.getId(), socket);
	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {
		GwtRpcSocket socket = openSockets.remove(session.getId());
		if (socket == null)
			throw new IllegalStateException("Socket not in the openSocket-list");
		socket.onClose(closeReason);
	}

	public static class GwtRpcSocket implements MessageHandler.Partial<String> {
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

		public void init(Session session, HttpSession httpSession) {
			this.session = session;
			this.httpSession = httpSession;
		}

		private boolean isInit = false;

		@Override
		public void onMessage(String data, boolean last) {
			logger.trace("Data recieved: {}", data);
			if (!isInit) {
				isInit = true;
				processInit(data);
			} else {
				manager.onCall(clientId, data, contextPath, permutationStrongName, moduleBasePath, httpSession);
			}
		}

		public void onClose(CloseReason closeReason) {
			// isConnected = false;
			logger.info("Client disconnected {}: {} (code: {})", session, closeReason.getReasonPhrase(), closeReason
					.getCloseCode().getCode());
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
							session.getBasicRemote().sendText(answer);
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

package com.googlecode.gwtrpcplus.server;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.websocket.DeploymentException;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.googlecode.gwtrpcplus.server.ModuleGwtRpcPlus.WebsocketModule;
import com.googlecode.gwtrpcplus.server.servlet.GwtRpcPlusWebsocket;
import com.googlecode.gwtrpcplus.server.util.Logger;

public class ModuleGwtRpcPlusWebsocket extends WebsocketModule {
	public static final String CONTEXT_PATH_NAME = "gwtrpcplus-websocket-servletContextPath";
	private final static Logger logger = new Logger(ModuleGwtRpcPlus.class);

	private boolean added = false;

	@Override
	public boolean isAdded() {
		return added;
	}

	@Override
	protected void configureServlets() {
		ServletContext servletContext = getServletContext();
		if (servletContext == null)
			logger.error("Websockets only works if you are using the GuiceServletContextListener to create your injector.");
		final ServerContainer serverContainer = (ServerContainer) servletContext.getAttribute("javax.websocket.server.ServerContainer");

		// @SuppressWarnings("unchecked")
		// Enumeration<String> attributeNames = servletContext.getAttributeNames();
		// while (attributeNames.hasMoreElements()) {
		// String attributeName = attributeNames.nextElement();
		// System.out.println(attributeName + " => " + servletContext.getAttribute(attributeName));
		// }

		if (serverContainer == null)
			logger.warn("No JSR-356 Websocket-Support found for " + servletContext.getServerInfo());
		else {
			try {
				serverContainer.setDefaultMaxTextMessageBufferSize(1000000);

				MyConfigurator configurator = new MyConfigurator(servletContext.getContextPath());
				requestInjection(configurator);

				ServerEndpointConfig cfg = ServerEndpointConfig.Builder.create(GwtRpcPlusWebsocket.class, getWebsocketPath()).configurator(configurator).build();
				serverContainer.addEndpoint(cfg);
				added = true;
			} catch (DeploymentException e) {
				logger.error("Error while deploying WebsocketEndpoint", e);
			}
			logger.info("Websocket-Support initialized.");
		}
	}

	private static class MyConfigurator extends ServerEndpointConfig.Configurator {
		@Inject
		private Injector injector;
		private String contextPath;

		public MyConfigurator(String contextPath) {
			this.contextPath = contextPath;
		}

		@Override
		public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
			T instance = super.getEndpointInstance(endpointClass);
			injector.injectMembers(instance);
			return instance;
		}

		@Override
		public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
			HttpSession httpSession = (HttpSession) request.getHttpSession();
			config.getUserProperties().put(HttpSession.class.getName(), httpSession);
			config.getUserProperties().put(CONTEXT_PATH_NAME, contextPath);
		}
	}
}

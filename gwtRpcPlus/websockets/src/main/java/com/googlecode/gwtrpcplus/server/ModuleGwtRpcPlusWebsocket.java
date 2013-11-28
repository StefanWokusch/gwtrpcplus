package com.googlecode.gwtrpcplus.server;

import javax.servlet.http.HttpSession;
import javax.websocket.DeploymentException;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.googlecode.gwtrpcplus.server.ModuleGwtRpcPlus.InternalGwtRpcPlusModule;
import com.googlecode.gwtrpcplus.server.servlet.GwtRpcPlusWebsocket;
import com.googlecode.gwtrpcplus.server.util.Logger;

public class ModuleGwtRpcPlusWebsocket extends InternalGwtRpcPlusModule {
	private final static Logger logger = new Logger(ModuleGwtRpcPlus.class);

	@Override
	protected void configureServlets() {
		System.out.println("---INIT");
		final ServerContainer serverContainer = (ServerContainer) getServletContext().getAttribute(
				"javax.websocket.server.ServerContainer");

		if (serverContainer == null)
			logger.warn("No Websocket-Support found.");
		else {
			try {
				String path = "/" + getModulename() + "/gwtRpcPlusWebsocket";
				MyConfigurator configurator = new MyConfigurator();
				requestInjection(configurator);
				ServerEndpointConfig cfg = ServerEndpointConfig.Builder.create(GwtRpcPlusWebsocket.class, path)
						.configurator(configurator).build();
				serverContainer.addEndpoint(cfg);
			} catch (DeploymentException e) {
				logger.error("Error while deploying WebsocketEndpoint", e);
			}
			logger.info("Websocket-Support initialized.");
		}
	}

	private static class MyConfigurator extends ServerEndpointConfig.Configurator {
		@Inject
		private Injector injector;

		@Override
		public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
			T instance = super.getEndpointInstance(endpointClass);
			injector.injectMembers(instance);

			System.out.println("INSTANCE:: " + instance.getClass());

			return instance;
		}

		@Override
		public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
			HttpSession httpSession = (HttpSession) request.getHttpSession();
			config.getUserProperties().put(HttpSession.class.getName(), httpSession);
			System.out.println("adding http-session");
		}
	}
}

package com.googlecode.gwtrpcplus.server;

import java.util.HashSet;
import java.util.Set;

import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import com.googlecode.gwtrpcplus.server.servlet.GwtRpcPlusBasicServlet;
import com.googlecode.gwtrpcplus.server.servlet.GwtRpcPlusBundleServlet;
import com.googlecode.gwtrpcplus.server.util.Logger;

public class ModuleGwtRpcPlus extends ServletModule {
	private final static Logger logger = new Logger(ModuleGwtRpcPlus.class);

	private final String modulename;
	private final Set<Class<? extends RemoteServiceServlet>> servletClasses;

	/**
	 * @param base
	 *          for example the projectName
	 * @param servletClasses
	 *          Set of all ServletClasses
	 */
	public ModuleGwtRpcPlus(String modulename,
			@SuppressWarnings("unchecked") Class<? extends RemoteServiceServlet>... servletClasses) {
		Set<Class<? extends RemoteServiceServlet>> classes = new HashSet<Class<? extends RemoteServiceServlet>>();
		for (Class<? extends RemoteServiceServlet> c : servletClasses)
			classes.add(c);

		this.modulename = modulename;
		this.servletClasses = classes;
	}

	/**
	 * @param base
	 *          for example the projectName
	 * @param servletClasses
	 *          Set of all ServletClasses
	 */
	public ModuleGwtRpcPlus(String modulename, Set<Class<? extends RemoteServiceServlet>> servletClasses) {
		this.modulename = modulename;
		this.servletClasses = servletClasses;
	}

	@Override
	protected final void configureServlets() {
		// Place for the user to add custom Code, when inherit from this Module
		configureCustomServlets();

		// Try adding Websocket-module
		tryAddInternalModule("com.googlecode.gwtrpcplus.server.ModuleGwtRpcPlusWebsocket");
		tryAddInternalModule("com.googlecode.gwtrpcplus.server.jetty.ModuleGwtRpcPlusWebsocket");

		// ConnectionBasic
		serve("/" + modulename + "/gwtRpcPlusBasic").with(GwtRpcPlusBasicServlet.class);

		// ConnectionBundle
		serve("/" + modulename + "/gwtRpcPlusBundle").with(GwtRpcPlusBundleServlet.class);

		bind(ServletList.class).toInstance(new ServletList() {
			@Override
			public Set<Class<? extends RemoteServiceServlet>> getServletClasses() {
				return servletClasses;
			}
		});
	}

	public static class InternalGwtRpcPlusModule extends ServletModule {
		private String modulename;

		public String getModulename() {
			return modulename;
		}

		public void setModulename(String modulename) {
			this.modulename = modulename;
		}
	}

	/**
	 * Adds an Module by its name. This is used to add Modules automaticly via classpath
	 * 
	 * @param internalModule
	 *          full qualified classname of the InternalGwtRpcPlusModule
	 */
	private void tryAddInternalModule(String internalModule) {
		try {
			InternalGwtRpcPlusModule m = (InternalGwtRpcPlusModule) Class.forName(internalModule).newInstance();
			m.setModulename(modulename);
			install(m);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			logger.info("WebsocketModule not added", e);
		}
	}

	protected void configureCustomServlets() {
	}

	/**
	 * Optional usage for adding the Service to gwtrpcplus and to the servlet.
	 * 
	 * It binds the Service as Singleton too.
	 * 
	 * @param clazz
	 *          ServiceImpl to add
	 */
	protected void bindGwtServlet(Class<? extends RemoteServiceServlet> clazz) {
		servletClasses.add(clazz);
		bind(clazz).in(Singleton.class);
		serve("/" + modulename + "/" + findName(clazz)).with(clazz);
	}

	private String findName(Class<?> clazz) {
		for (Class<?> c : clazz.getInterfaces())
			if (c.isAnnotationPresent(RemoteServiceRelativePath.class))
				return c.getAnnotation(RemoteServiceRelativePath.class).value();
		return findName(clazz.getSuperclass());
	}
}

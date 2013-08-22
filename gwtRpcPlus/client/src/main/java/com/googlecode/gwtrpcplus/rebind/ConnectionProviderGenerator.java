package com.googlecode.gwtrpcplus.rebind;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.googlecode.gwtrpcplus.client.Connection;
import com.googlecode.gwtrpcplus.client.connection.ConnectionHttp;
import com.googlecode.gwtrpcplus.client.connection.ConnectionHttpBundle;
import com.googlecode.gwtrpcplus.client.connection.ConnectionWebsocket;

public class ConnectionProviderGenerator extends Generator {
	public String generate(TreeLogger logger, GeneratorContext context, String typeName) throws UnableToCompleteException {
		TypeOracle typeOracle = context.getTypeOracle();
		try {
			JClassType classType = typeOracle.getType(typeName);
			String packageName = classType.getPackage().getName();
			String className = classType.getSimpleSourceName() + "Impl";
			generateClass(logger, context, classType, packageName, className);
			return packageName + "." + className;
		} catch (Exception e) {
			logger.log(TreeLogger.ERROR, "Cant create Factory for " + typeName + ": " + e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Generate source code for new class. Class extends <code>HashMap</code>.
	 * 
	 * @param logger
	 *          Logger object
	 * @param context
	 *          Generator context
	 * @throws BadPropertyValueException
	 */
	private void generateClass(TreeLogger logger, GeneratorContext context, JClassType classType, String packageName,
			String className) throws BadPropertyValueException {
		PrintWriter printWriter = context.tryCreate(logger, packageName, className);
		if (printWriter == null)
			return;
		ClassSourceFileComposerFactory composer = new ClassSourceFileComposerFactory(packageName, className);
		composer.addImport(List.class.getName());
		composer.addImport(ArrayList.class.getName());
		composer.addImport(Connection.class.getName());
		composer.addImplementedInterface(classType.getQualifiedSourceName());

		SourceWriter sourceWriter = composer.createSourceWriter(context, printWriter);

		sourceWriter.println("@Override");
		sourceWriter.println("public List<Connection> get() {");
		sourceWriter.indent();
		sourceWriter.println("ArrayList<Connection> ret = new ArrayList<Connection>();");

		// High Prio first -> Low Prio later
		if (context.getPropertyOracle().getConfigurationProperty("gwtrpcplus_websockets_enabled").getValues().get(0)
				.equalsIgnoreCase("true")) {
			sourceWriter.println("ret.add(new " + ConnectionWebsocket.class.getName() + "());");
		}

		if (context.getPropertyOracle().getConfigurationProperty("gwtrpcplus_bundleHttpRequests").getValues().get(0)
				.equalsIgnoreCase("true")) {
			sourceWriter.println("ret.add(new " + ConnectionHttpBundle.class.getName() + "());");
		} else
			sourceWriter.println("ret.add(new " + ConnectionHttp.class.getName() + "());");

		sourceWriter.println("return ret;");
		sourceWriter.outdent();
		sourceWriter.println("}");

		sourceWriter.outdent();
		sourceWriter.println("}");
		context.commit(logger, printWriter);
	}
}

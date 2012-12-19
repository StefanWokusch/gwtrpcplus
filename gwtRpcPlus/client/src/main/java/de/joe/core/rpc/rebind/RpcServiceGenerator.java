package de.joe.core.rpc.rebind;

import java.util.HashMap;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.user.client.rpc.impl.RemoteServiceProxy;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.rpc.ProxyCreator;
import com.google.gwt.user.rebind.rpc.ServiceInterfaceProxyGenerator;

import de.joe.core.rpc.client.RequestMethod;
import de.joe.core.rpc.client.impl.GwtRpcProxy;
import de.joe.core.rpc.client.impl.RequestMethodBasic;
import de.joe.core.rpc.client.impl.RequestMethodQueued;
import de.joe.core.rpc.client.impl.RequestMethodServerpush;
import de.joe.core.rpc.shared.Queued;
import de.joe.core.rpc.shared.RemoteServicePlus;
import de.joe.core.rpc.shared.ResendAllowed;
import de.joe.core.rpc.shared.ServerPush;

/**
 * Creates the Proxy for the Services like the Original but with my Proxy
 */
public class RpcServiceGenerator extends ServiceInterfaceProxyGenerator {
  @Override
  protected ProxyCreator createProxyCreator(JClassType remoteService) {
    if (doProxy)
      return new MyProxyCreator(remoteService);
    return super.createProxyCreator(remoteService);
  }

  private boolean doProxy;

  @Override
  public RebindResult generateIncrementally(TreeLogger logger, GeneratorContext ctx, String requestedClass)
      throws UnableToCompleteException {

    try {
      JClassType plusinterface = ctx.getTypeOracle().findType(RemoteServicePlus.class.getName());
      JClassType requested = ctx.getTypeOracle().findType(requestedClass);
      boolean isPlusInterface = requested.isAssignableTo(plusinterface);
      boolean noRemoteService = ctx.getPropertyOracle().getConfigurationProperty(
          "gwtrpcplus_websockets_NoRemoveService").getValues().get(0).equals("true");
      doProxy = !noRemoteService || isPlusInterface;

    } catch (BadPropertyValueException e) {
      logger.log(Type.ERROR, e.getMessage());
    }

    return super.generateIncrementally(logger, ctx, requestedClass);
  }

  public static class MyProxyCreator extends ProxyCreator {
    public MyProxyCreator(JClassType type) {
      super(type);
    }

    @Override
    protected Class<? extends RemoteServiceProxy> getProxySupertype() {
      return GwtRpcProxy.class;
    }


    @Override
    protected void generateProxyContructor(SourceWriter srcWriter) {
      srcWriter.println("@Override");
      srcWriter.println("protected void addMethods(" + HashMap.class.getName() + "<String, "
          + RequestMethod.class.getName() + "> methods) {");
      srcWriter.indent();

      for (JMethod m : serviceIntf.getOverridableMethods()) {
        String resendAllowedValue = m.isAnnotationPresent(ResendAllowed.class) ? "true" : "false";
        String serviceNameValue = "\"" + serviceIntf.getSimpleSourceName() + "\"";
        boolean isServerPush = m.isAnnotationPresent(ServerPush.class);
        boolean isQueued = m.isAnnotationPresent(Queued.class);

        if (isServerPush && isQueued) {
          throw new IllegalArgumentException("Methods can't be ServerPush AND Queued (" + m + ")");
        }

        if (isServerPush) {
          srcWriter.println("methods.put(\"" + m.getName() + "\", new " + RequestMethodServerpush.class.getName() + "("
              + serviceNameValue + ", " + resendAllowedValue + "));");
        } else if (isQueued) {
          Queued queued = m.getAnnotation(Queued.class);
          srcWriter.println("methods.put(\"" + m.getName() + "\", new " + RequestMethodQueued.class.getName() + "("
              + serviceNameValue + ", " + queued.value() + ", " + resendAllowedValue + "));");
        } else {
          srcWriter.println("methods.put(\"" + m.getName() + "\", new " + RequestMethodBasic.class.getName() + "("
              + serviceNameValue + ", " + resendAllowedValue + "));");
        }
      }
      srcWriter.outdent();
      srcWriter.println("}");

      super.generateProxyContructor(srcWriter);
    }
  }
}

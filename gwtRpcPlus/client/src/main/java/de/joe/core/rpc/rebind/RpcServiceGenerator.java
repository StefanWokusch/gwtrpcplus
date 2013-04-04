package de.joe.core.rpc.rebind;

import java.util.HashMap;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.user.client.rpc.impl.RemoteServiceProxy;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.rpc.ProxyCreator;
import com.google.gwt.user.rebind.rpc.ServiceInterfaceProxyGenerator;

import de.joe.core.rpc.client.RequestMethod;
import de.joe.core.rpc.client.impl.GwtRpcProxy;
import de.joe.core.rpc.client.type.RequestMethodBasic;
import de.joe.core.rpc.client.type.RequestMethodQueued;
import de.joe.core.rpc.client.type.RequestMethodServerpush;
import de.joe.core.rpc.shared.Queued;
import de.joe.core.rpc.shared.ResendAllowed;
import de.joe.core.rpc.shared.ServerPush;

/**
 * Creates the Proxy for the Services like the Original but with my Proxy
 */
public class RpcServiceGenerator extends ServiceInterfaceProxyGenerator {
  @Override
  protected ProxyCreator createProxyCreator(JClassType remoteService) {
    return new MyProxyCreator(remoteService);
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

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
import de.joe.core.rpc.client.impl.RequestMethodBasic;
import de.joe.core.rpc.client.impl.RequestMethodServerpush;
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
        if (m.isAnnotationPresent(ServerPush.class))
          srcWriter.println("methods.put(\"" + m.getName() + "\", new " + RequestMethodServerpush.class.getName()
              + "(\"" + serviceIntf.getSimpleSourceName() + "\"));");
        else
          srcWriter.println("methods.put(\"" + m.getName() + "\", new " + RequestMethodBasic.class.getName() + "(\""
              + serviceIntf.getSimpleSourceName() + "\"));");
      }
      srcWriter.outdent();
      srcWriter.println("}");

      super.generateProxyContructor(srcWriter);
    }
  }
}

package de.joe.core.rpc.rebind;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.client.rpc.impl.RemoteServiceProxy;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.rpc.ProxyCreator;
import com.google.gwt.user.rebind.rpc.SerializableTypeOracle;
import com.google.gwt.user.rebind.rpc.ServiceInterfaceProxyGenerator;

import de.joe.core.rpc.client.impl.GwtRpcProxy;

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
      srcWriter.println("protected String[] noBundles() {");
      srcWriter.indent();
      srcWriter.println("return new String[] {");
      srcWriter.indent();
//      for (JMethod m : serviceIntf.getOverridableMethods()) {
//        if (m.isAnnotationPresent(NoBundle.class))
//          srcWriter.println("\"" + getProxySimpleName() + "." + m.getName() + "\"");
//      }
      srcWriter.outdent();
      srcWriter.println("};");
      srcWriter.outdent();
      srcWriter.println("}");

      super.generateProxyContructor(srcWriter);
    }

    @Override
    protected void generateProxyMethod(final SourceWriter w, SerializableTypeOracle serializableTypeOracle,
        TypeOracle typeOracle, JMethod syncMethod, JMethod asyncMethod) {
      super.generateProxyMethod(w, serializableTypeOracle, typeOracle, syncMethod, asyncMethod);
    }
  }
}

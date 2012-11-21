package de.joe.core.rpc.client.impl;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.impl.RemoteServiceProxy;
import com.google.gwt.user.client.rpc.impl.RequestCallbackAdapter.ResponseReader;
import com.google.gwt.user.client.rpc.impl.RpcStatsContext;
import com.google.gwt.user.client.rpc.impl.Serializer;

import de.joe.core.rpc.client.RequestMethod;
import de.joe.core.rpc.client.RpcManagerClient;

/**
 * The Class the AsyncImpls will extend from
 */
public abstract class GwtRpcProxy extends RemoteServiceProxy {

  protected GwtRpcProxy(String moduleBaseURL, String remoteServiceRelativePath, String serializationPolicyName,
      Serializer serializer) {
    super(moduleBaseURL, remoteServiceRelativePath, serializationPolicyName, serializer);
    // RpcWebsocket.verifyInit();
    // for (String s : noBundles())
    // noBundles.add(s);
  }

  // private final HashSet<String> noBundles = new HashSet<String>();

  protected abstract String[] noBundles();

  @Override
  protected <T> Request doInvoke(ResponseReader responseReader, String methodName, RpcStatsContext statsContext,
      String requestData, AsyncCallback<T> callback) {

    RequestCallback requestCallback = doCreateRequestCallback(responseReader, methodName, statsContext, callback);
    // methodname starts with the servicename the _Proxy of this overwritten AsyncImpl
    String service = methodName.substring(0, methodName.lastIndexOf("_Proxy"));

    // FIXME generate these (and use singletons
    RequestMethod method = new RequestMethodBasic(service);

    return RpcManagerClient.get().call(method, requestData, requestCallback);

    // // Check for websockets
    // if (RpcWebsocket.isConnected()) {
    // return RpcWebsocket.doInvokeByWebSocket(service, requestData, requestCallback);
    // } else if (!noBundles.contains(methodName)) {
    // return RpcBundle.doInvokeByBundle(service, requestData, requestCallback);
    // } else {
    // return super.doInvoke(responseReader, methodName, statsContext, requestData, callback);
    // }
  }

}

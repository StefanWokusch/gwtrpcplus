package com.googlecode.gwtrpcplus.client.impl;

import java.util.HashMap;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.impl.RemoteServiceProxy;
import com.google.gwt.user.client.rpc.impl.RequestCallbackAdapter.ResponseReader;
import com.google.gwt.user.client.rpc.impl.RpcStatsContext;
import com.google.gwt.user.client.rpc.impl.Serializer;
import com.googlecode.gwtrpcplus.client.RequestMethod;
import com.googlecode.gwtrpcplus.client.RpcManagerClient;


/**
 * The Class the AsyncImpls will extend from
 */
public abstract class GwtRpcProxy extends RemoteServiceProxy {

  protected GwtRpcProxy(String moduleBaseURL, String remoteServiceRelativePath, String serializationPolicyName,
      Serializer serializer) {
    super(moduleBaseURL, remoteServiceRelativePath, serializationPolicyName, serializer);
    methods = new HashMap<String, RequestMethod>();
    addMethods(methods);
  }

  private final HashMap<String, RequestMethod> methods;

  protected abstract void addMethods(HashMap<String, RequestMethod> methods);

  @Override
  protected <T> Request doInvoke(ResponseReader responseReader, String methodName, RpcStatsContext statsContext,
      String requestData, AsyncCallback<T> callback) {

    String realMethodName = methodName.substring(methodName.lastIndexOf(".") + 1);

    RequestMethod method = methods.get(realMethodName);
    assert (method != null) : "Method " + realMethodName + " of " + this.getClass() + " isn't registrerd";

    RequestCallback requestCallback = doCreateRequestCallback(responseReader, methodName, statsContext, callback);

    return RpcManagerClient.get().call(method, requestData, requestCallback);
  }

}

package com.googlecode.gwtrpcplus.shared;

import com.google.gwt.user.client.rpc.RemoteService;

/**
 * Marker for Serivces, needed to use the GwtRpcPlus Framework.
 * 
 * For example ServerPush or ResendAllowed Features.
 * 
 * By Default, all RemoteServices will be use the GwtRpcPlus, but it can be configured, to use only
 * the RemoteServicePlus values. See Documentation at GoogleCode for more Informations.
 */
public interface RemoteServicePlus extends RemoteService {
}

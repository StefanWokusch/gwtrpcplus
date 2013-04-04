package de.joe.core.rpc.client.util.websocket;

import com.google.gwt.core.client.GWT;

public class WebSocket {
	public static interface Callback {
	    void onOpen();
	    void onClose();
	    void onError(Object e);
	    void onMessage(String message);
	}
	
    private final Callback callback;

	private Object ws;
    
    public WebSocket(Callback callback) {
        this.callback = callback;
    }

    private boolean isConnected=false;
    private final void onopen() {
    	isConnected=true;
    	try{
    		callback.onOpen();
    	}catch (Throwable e) {
    		if(GWT.getUncaughtExceptionHandler()!=null)
    			GWT.getUncaughtExceptionHandler().onUncaughtException(e);
    		else
    			GWT.log("Error",e);
		}
    }

    private final void onclose() {
    	isConnected=false;
    	ws=null;

    	try{
    		callback.onClose();
    	}catch (Throwable e) {
    		if(GWT.getUncaughtExceptionHandler()!=null)
    			GWT.getUncaughtExceptionHandler().onUncaughtException(e);
    		else
    			GWT.log("Error",e);
		}
    }

    private final void onmessage(String message) {
    	try{
    		callback.onMessage(message);
    	}catch (Throwable e) {
    		if(GWT.getUncaughtExceptionHandler()!=null)
    			GWT.getUncaughtExceptionHandler().onUncaughtException(e);
    		else
    			GWT.log("Error",e);
		}
    }

    private final void onerror(Object error) {
    	try{
    		callback.onError(error);
    	}catch (Throwable e) {
    		if(GWT.getUncaughtExceptionHandler()!=null)
    			GWT.getUncaughtExceptionHandler().onUncaughtException(e);
    		else
    			GWT.log("Error",e);
		}
    }

    public static native boolean isSupported()/*-{
    	return "WebSocket" in $wnd || "MozWebSocket" in $wnd;
    }-*/;
    
    public boolean isConnected(){
    	return isSupported() && isConnected;
    }


    
    public native void connect(String serverUrl) /*-{
    	var that = this;
    	
    	if("WebSocket" in $wnd)
    		this.@de.joe.core.rpc.client.util.websocket.WebSocket::ws = new WebSocket(serverUrl);
    	else
    		this.@de.joe.core.rpc.client.util.websocket.WebSocket::ws = new MozWebSocket(serverUrl);
    		

        this.@de.joe.core.rpc.client.util.websocket.WebSocket::ws.onopen = function() {
             that.@de.joe.core.rpc.client.util.websocket.WebSocket::onopen()();
        };
        this.@de.joe.core.rpc.client.util.websocket.WebSocket::ws.onmessage = function(response) {
            if (response.data) {
                that.@de.joe.core.rpc.client.util.websocket.WebSocket::onmessage(Ljava/lang/String;)( response.data );
            }
        };
        this.@de.joe.core.rpc.client.util.websocket.WebSocket::ws.onclose = function(m) {
             that.@de.joe.core.rpc.client.util.websocket.WebSocket::onclose()();
        };
        this.@de.joe.core.rpc.client.util.websocket.WebSocket::ws.onerror = function(m) {
             that.@de.joe.core.rpc.client.util.websocket.WebSocket::onerror(Ljava/lang/Object;)(m);
        };
    }-*/;

    public native void send(String message) /*-{
        if (this.@de.joe.core.rpc.client.util.websocket.WebSocket::ws) {
            this.@de.joe.core.rpc.client.util.websocket.WebSocket::ws.send(message);
        } else {
            alert("not connected!" + this.@de.joe.core.rpc.client.util.websocket.WebSocket::ws);
        }
    }-*/;

    public native void close() /*-{
        this.@de.joe.core.rpc.client.util.websocket.WebSocket::ws.close();
    }-*/;

}
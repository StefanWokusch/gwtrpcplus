package de.joe.core.rpc.client.util;

import com.google.gwt.http.client.Header;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;

public class RequestHelper {
	public static void process(RequestCallback requestCallback, final String result){
		Request req=new Request(){};
		Response res=new Response() {
			@Override
			public String getText() {
				return result;
			}
			@Override
			public String getStatusText() {
				return null;
			}
			
			@Override
			public int getStatusCode() {
				return Response.SC_OK;
			}
			
			@Override
			public String getHeadersAsString() {
				return null;
			}
			
			@Override
			public Header[] getHeaders() {
				return null;
			}
			
			@Override
			public String getHeader(String header) {
				return null;
			}
		};
		requestCallback.onResponseReceived(req, res);
	}
	
	
	
}

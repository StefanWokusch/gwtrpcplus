package com.googlecode.gwtrpcplus.client.type;

import com.googlecode.gwtrpcplus.client.RequestMethod.RequestPlus;

public abstract class AbstractRequestPlus implements RequestPlus {

	private String id;

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

}

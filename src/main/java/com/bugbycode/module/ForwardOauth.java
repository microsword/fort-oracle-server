package com.bugbycode.module;

import java.io.Serializable;

public class ForwardOauth implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2065900452622926934L;

	private String clientId;
	
	private String clientSecret;
	
	private String scope;
	
	private String tokenUrl;
	
	private String refreshTokenUrl;
	
	private String checkTokenUrl;

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String getTokenUrl() {
		return tokenUrl;
	}

	public void setTokenUrl(String tokenUrl) {
		this.tokenUrl = tokenUrl;
	}

	public String getRefreshTokenUrl() {
		return refreshTokenUrl;
	}

	public void setRefreshTokenUrl(String refreshTokenUrl) {
		this.refreshTokenUrl = refreshTokenUrl;
	}

	public String getCheckTokenUrl() {
		return checkTokenUrl;
	}

	public void setCheckTokenUrl(String checkTokenUrl) {
		this.checkTokenUrl = checkTokenUrl;
	}
}

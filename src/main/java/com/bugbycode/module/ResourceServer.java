package com.bugbycode.module;

import java.io.Serializable;

public class ResourceServer implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8108694164220714207L;

	private String userServerUrl;
	
	private String resourceServerUrl;
	
	private String ruleServerUrl;
	
	private String logServerUrl;
	
	private String ssoServerUrl;

	public String getUserServerUrl() {
		return userServerUrl;
	}

	public void setUserServerUrl(String userServerUrl) {
		this.userServerUrl = userServerUrl;
	}

	public String getResourceServerUrl() {
		return resourceServerUrl;
	}

	public void setResourceServerUrl(String resourceServerUrl) {
		this.resourceServerUrl = resourceServerUrl;
	}

	public String getRuleServerUrl() {
		return ruleServerUrl;
	}

	public void setRuleServerUrl(String ruleServerUrl) {
		this.ruleServerUrl = ruleServerUrl;
	}

	public String getLogServerUrl() {
		return logServerUrl;
	}

	public void setLogServerUrl(String logServerUrl) {
		this.logServerUrl = logServerUrl;
	}

	public String getSsoServerUrl() {
		return ssoServerUrl;
	}

	public void setSsoServerUrl(String ssoServerUrl) {
		this.ssoServerUrl = ssoServerUrl;
	}
}

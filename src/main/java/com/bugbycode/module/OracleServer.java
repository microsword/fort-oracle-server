package com.bugbycode.module;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class OracleServer implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1670011021049301626L;

	private String loginName;
	
	private String password;
	
	private String oracleHost;
	
	private String oracleAccount;
	
	private String oraclePassword;
	
	private int oraclePort;
	
	private String serviceName;
	
	private String body;
	
	public OracleServer() {
		
	}
	
	public OracleServer(String loginName, String password, String oracleHost, 
			String oracleAccount, int oraclePort) {
		this.loginName = loginName;
		this.password = password;
		this.oracleHost = oracleHost;
		this.oracleAccount = oracleAccount;
		this.oraclePort = oraclePort;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getOracleHost() {
		return oracleHost;
	}

	public void setOracleHost(String oracleHost) {
		this.oracleHost = oracleHost;
	}

	public String getOracleAccount() {
		return oracleAccount;
	}

	public void setOracleAccount(String oracleAccount) {
		this.oracleAccount = oracleAccount;
	}

	public int getOraclePort() {
		return oraclePort;
	}

	public void setOraclePort(int oraclePort) {
		this.oraclePort = oraclePort;
	} 
	
	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getOraclePassword() {
		return oraclePassword;
	}

	public void setOraclePassword(String oraclePassword) {
		this.oraclePassword = oraclePassword;
	}

	@Override
	public String toString() {
		JSONObject json = new JSONObject();
		try {
			json.put("loginName", loginName);
			json.put("password", password);
			json.put("oracleHost", oracleHost);
			json.put("oraclePort", oraclePort);
			json.put("oracleAccount", oracleAccount);
			json.put("oraclePassword", oraclePassword);
			json.put("serviceName", serviceName);
			json.put("body", body);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json.toString();
	}
}

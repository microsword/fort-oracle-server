package com.bugbycode.service.oracle.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bugbycode.config.AppConfig;
import com.bugbycode.module.ForwardOauth;
import com.bugbycode.module.OracleServer;
import com.bugbycode.module.ResourceServer;
import com.bugbycode.service.oracle.OracleHostFormatService;
import com.util.TransferUtil;
import com.util.http.HttpClient;

public class OracleHostFormatServiceImpl implements OracleHostFormatService {

	private ForwardOauth forwardOauth;
	
	private ResourceServer resourceServer;
	
	private HttpClient httpClient;
	
	public OracleHostFormatServiceImpl(ForwardOauth forwardOauth,ResourceServer resourceServer) {
		this.forwardOauth = forwardOauth;
		this.resourceServer = resourceServer;
		this.httpClient = new HttpClient();
	}
	
	@Override
	public OracleServer format(byte[] buff) throws IOException {
		
		int port = 0;
		String service = "";
		
		int keyIndex = TransferUtil.findKey(AppConfig.CONNECTION_KEY, buff);
		
		byte[] conn_byte_arr = new byte[buff.length - keyIndex];
		
		System.arraycopy(buff, keyIndex, conn_byte_arr, 0, conn_byte_arr.length);
		
		String body = new String(conn_byte_arr);
		
		int serviceIndex = body.indexOf(AppConfig.SERVICE_NAME_KEY);
		int endIndex = body.indexOf(')', serviceIndex);
		String serviceName = body.substring(serviceIndex + AppConfig.SERVICE_NAME_KEY.length(), endIndex);
		
		String tokenJsonStr = httpClient.getToken(forwardOauth.getTokenUrl(), "client_credentials", forwardOauth.getClientId(), forwardOauth.getClientSecret(), forwardOauth.getScope());
		JSONObject tokenJson = JSONObject.parseObject(tokenJsonStr);
		if(tokenJson.containsKey("error")) {
			throw new IOException(tokenJson.getString("error_description"));
		}

		String token = tokenJson.getString("access_token");
		Map<String,Object> data = new HashMap<String,Object>();
		data.put("random", serviceName);
		
		String result = httpClient.getResource(resourceServer.getSsoServerUrl() + AppConfig.QUERY_SESSION_BY_RANDOM_PATH, token, data);
		JSONObject resultJson = JSONObject.parseObject(result);
		if(resultJson.containsKey("error")) {
			throw new IOException(resultJson.getString("error_description"));
		}
		
		if(!resultJson.containsKey("data")) {
			throw new IOException("SERVICE_NAME \"" + serviceName + "\" ERROR.");
		}
		
		resultJson = resultJson.getJSONObject("data");
		
		int resId = resultJson.getIntValue("resId");
		
		String userName = resultJson.getString("userName");
		
		String account = resultJson.getString("account");
		
		data.clear();
		data.put("resId", resId);
		result = httpClient.getResource(resourceServer.getResourceServerUrl() + AppConfig.GET_RESOURCE_BY_RESID_PATH, token, data);
		resultJson = JSONObject.parseObject(result);
		if(!resultJson.containsKey("data")) {
			throw new IOException("SERVICE_NAME \"" + serviceName + "\" ERROR.");
		}
		
		resultJson = resultJson.getJSONObject("data");
		
		String host = resultJson.getString("ip");
		
		data.clear();
		data.put("resId", resId);
		data.put("serverType", 4);
		result = httpClient.getResource(resourceServer.getResourceServerUrl() + AppConfig.GET_SSO_INFO_PATH, token, data);
		resultJson = JSONObject.parseObject(result);
		
		if(!resultJson.containsKey("data")) {
			throw new IOException("SERVICE_NAME \"" + serviceName + "\" ERROR.");
		}
		
		resultJson = resultJson.getJSONObject("data");
		port = resultJson.getIntValue("port");
		int use = resultJson.getIntValue("use");
		service = resultJson.getString("database");
		
		if(use == 0) {
			throw new IOException("SERVICE_NAME \"" + serviceName + "\" ERROR.");
		}
		
		JSONArray accountArr = resultJson.getJSONArray("account");
		if(accountArr.isEmpty()) {
			throw new IOException("SERVICE_NAME \"" + serviceName + "\" ERROR.");
		}
		
		String oraclePassword = "";
		
		for(int i = 0;i < accountArr.size();i++) {
			JSONObject accObj = accountArr.getJSONObject(i);
			if(account.equals(accObj.getString("account"))) {
				oraclePassword = accObj.getString("password");
				break;
			}
		}
		
		data.clear();
		data.put("userName", userName);
		result = httpClient.getResource(resourceServer.getUserServerUrl() + AppConfig.GET_USER_BY_USERNAME_PATH, token, data);
		resultJson = JSONObject.parseObject(result);
		if(!resultJson.containsKey("data")) {
			throw new IOException("SERVICE_NAME \"" + serviceName + "\" ERROR.");
		}
		
		resultJson = resultJson.getJSONObject("data");
		
		String userPassword = resultJson.getString("password");
		
		int status = resultJson.getIntValue("status");
		
		if(status == 0) {
			throw new IOException(userName + "\" locked.");
		}
		
		body = body.substring(0, serviceIndex + AppConfig.SERVICE_NAME_KEY.length()) + service + body.substring(serviceIndex + AppConfig.SERVICE_NAME_KEY.length() + serviceName.length());
		
		int hostLastIndex = body.lastIndexOf(AppConfig.HOST_KEY);
		endIndex = body.indexOf(')', hostLastIndex);
		body = body.substring(0, hostLastIndex + AppConfig.HOST_KEY.length()) + host + body.substring(endIndex);
		
		int portLastIndex = body.lastIndexOf(AppConfig.PORT_KEY);
		endIndex = body.indexOf(')', portLastIndex);
		body = body.substring(0, portLastIndex + AppConfig.PORT_KEY.length()) + port + body.substring(endIndex);
		
		OracleServer server = new OracleServer();
		server.setLoginName(userName);
		server.setPassword(userPassword);
		server.setOracleHost(host);
		server.setOracleAccount(account);
		server.setOraclePassword(oraclePassword);
		server.setOraclePort(port);
		server.setServiceName(service);
		server.setBody(body);
		return server;
	}

}

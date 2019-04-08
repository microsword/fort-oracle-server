package com.util.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import sun.misc.BASE64Encoder;

import java.net.HttpURLConnection;

public class HttpClient {
	
	public HttpURLConnection getHttpURLConnection(String url) throws MalformedURLException, IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		return conn;
	}
	
	public String getToken(String url,String grant_type,String username,String password,String client_id,
			String client_secret,String scope) {
		StringBuilder build = new StringBuilder();
		build.append("grant_type=" + grant_type);
		build.append("&username=" + username);
		build.append("&password=" + password);
		build.append("&client_id=" + client_id);
		build.append("&client_secret=" + client_secret);
		build.append("&scope=" + scope);
		
		return sendData(url,null,build.toString().getBytes(),null);
	}
	
	public String getToken(String url,String grant_type,String client_id,String client_secret,String scope) {
		
		StringBuilder build = new StringBuilder();
		build.append("grant_type=" + grant_type);
		build.append("&client_id=" + client_id);
		build.append("&client_secret=" + client_secret);
		build.append("&scope=" + scope);
		
		return sendData(url,null,build.toString().getBytes(),null);
	}
	
	public String refreshToken(String url,String grant_type,String client_id,
			String client_secret,String refresh_token) {
		StringBuilder build = new StringBuilder();
		build.append("grant_type=" + grant_type);
		build.append("&client_id=" + client_id);
		build.append("&client_secret=" + client_secret);
		build.append("&refresh_token=" + refresh_token);
		return sendData(url,null,build.toString().getBytes(),null);
	}
	
	public String getResource(String url,String token,Map<String,Object> data) {
		StringBuilder builder = new StringBuilder();
		Iterator<Entry<String,Object>> it = data.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String,Object> entry = it.next();
			String key = entry.getKey();
			String value = entry.getValue().toString();
			if(builder.length() > 0) {
				builder.append('&');
			}
			builder.append(key);
			builder.append("=");
			builder.append(value);
		}
		return sendData(url, null, builder.toString().getBytes(), token);
	}

	
	public String checkToken(String url,String token,String clientId,String clientSecret) {
		StringBuilder build = new StringBuilder();
		build.append("token=" + token);
		return sendData(url,clientId + ":" + clientSecret,build.toString().getBytes(),null);
	}
	
	public String sendData(String url,String auth,byte[] data,String token) {
		HttpURLConnection conn = null;
		InputStream in = null;
		OutputStream out = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		
		StringBuilder build = new StringBuilder();
		
		try {
			conn = getHttpURLConnection(url);
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setConnectTimeout(5000);
			if(auth != null) {
				conn.setRequestProperty("accept", "*/*");
				conn.setRequestProperty("connection", "Keep-Alive");
				conn.setRequestProperty("Authorization", "Basic " + new BASE64Encoder().encode(auth.getBytes()));
			}else if(token != null) {
				conn.setRequestProperty("Authorization", "Bearer " + token);
			}
			
			out = conn.getOutputStream();
			out.write(data);
			out.flush();
			int code = conn.getResponseCode();
			if(code == 200) {
				in = conn.getInputStream();
			}else {
				in = conn.getErrorStream();
			}
			isr = new InputStreamReader(in, "UTF-8");
			br = new BufferedReader(isr);
			
			String line = null;
			while((line = br.readLine()) != null) {
				build.append(line);
			}
			return build.toString();
		} catch (MalformedURLException e) {
			//e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (IOException e) {
			//e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} finally {
			try {
				if(br != null) {
					br.close();
				}
				
				if(isr != null) {
					isr.close();
				}
				
				if(in != null) {
					in.close();
				}
				
				if(out != null) {
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(conn != null) {
				conn.disconnect();
			}
		}
	}
}

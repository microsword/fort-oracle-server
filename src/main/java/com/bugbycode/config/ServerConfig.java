package com.bugbycode.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bugbycode.module.ForwardOauth;
import com.bugbycode.module.ResourceServer;

@Configuration
public class ServerConfig {

	@Bean("forwardOauth")
	@ConfigurationProperties(prefix="spring.oracle-forward.oauth")
	public ForwardOauth getForwardOauth() {
		return new ForwardOauth();
	}
	
	@Bean("resourceServer")
	@ConfigurationProperties(prefix="spring.oracle-forward.server")
	public ResourceServer getReSourceServer() {
		return new ResourceServer();
	}
}

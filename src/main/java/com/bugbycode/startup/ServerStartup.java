package com.bugbycode.startup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.bugbycode.forward.server.startup.OracleServerRunnable;

import io.netty.channel.ChannelInboundHandler;

@Component
@Configuration
public class ServerStartup implements ApplicationRunner {

	@Value("${spring.netty.oracle.server.port}")
	private int oracleServerPort;
	
	@Autowired
	private ChannelInboundHandler oracleServerInitializer;
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		new OracleServerRunnable(oracleServerPort,oracleServerInitializer).run();
	}

}

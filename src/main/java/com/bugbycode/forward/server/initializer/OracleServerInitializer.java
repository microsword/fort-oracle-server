package com.bugbycode.forward.server.initializer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bugbycode.forward.decode.OracleDecode;
import com.bugbycode.forward.server.handler.OracleServerHandler;
import com.bugbycode.module.ForwardOauth;
import com.bugbycode.module.ResourceServer;

import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

@Service("oracleServerInitializer")
public class OracleServerInitializer extends ChannelInitializer<SocketChannel> {

	@Autowired
	private ForwardOauth forwardOauth;
	
	@Autowired
	private ResourceServer resourceServer;
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline p = ch.pipeline();
		ch.config().setAllocator(UnpooledByteBufAllocator.DEFAULT);
		p.addLast(new OracleDecode());
		p.addLast(new OracleServerHandler(forwardOauth,resourceServer));
	}

}

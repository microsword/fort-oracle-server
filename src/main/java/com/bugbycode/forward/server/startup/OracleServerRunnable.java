package com.bugbycode.forward.server.startup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class OracleServerRunnable implements Runnable {

	private final Logger logger = LogManager.getLogger(OracleServerRunnable.class);
	
	private int serverPort;
	
	private EventLoopGroup boss;
	
	private EventLoopGroup worker;
	
	private ChannelInboundHandler oracleServerInitializer;
	
	public OracleServerRunnable(int serverPort,
			ChannelInboundHandler oracleServerInitializer) {
		this.serverPort = serverPort;
		this.oracleServerInitializer = oracleServerInitializer;
	}

	@Override
	public void run() {
		ServerBootstrap bootstrap = new ServerBootstrap();
		boss = new NioEventLoopGroup();
		worker = new NioEventLoopGroup();
		bootstrap.group(boss, worker).channel(NioServerSocketChannel.class)
		.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
		.option(ChannelOption.SO_BACKLOG, 5000)
		.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
		.childOption(ChannelOption.SO_KEEPALIVE, true)
		.childHandler(oracleServerInitializer);
		
		bootstrap.bind(serverPort).addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					logger.info("Oracle server startup successfully, port " + serverPort + "......");
				}else {
					close();
					logger.info("Oracle server startup failed, port " + serverPort + "......");
				}
			}
			
		});
	}

	public void close() {
		if(boss != null) {
			boss.shutdownGracefully();
		}
		
		if(worker != null) {
			worker.shutdownGracefully();
		}
		
		logger.info("Oracle server shutdown, port " + serverPort + "......");
	}
}

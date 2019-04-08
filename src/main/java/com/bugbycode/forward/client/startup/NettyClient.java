package com.bugbycode.forward.client.startup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.forward.client.handler.ClientHandler;
import com.bugbycode.forward.decode.OracleDecode;
import com.bugbycode.forward.server.handler.OracleServerHandler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyClient {
	
	private final Logger logger = LogManager.getLogger(NettyClient.class);
	
	private OracleServerHandler oracleServerHandler;
	
	private Channel clientChannel;
	
	private boolean isFinish;
	
	private boolean isClosed;
	
	private Bootstrap remoteClient;
	
	private EventLoopGroup remoteGroup;
	
	private String host;
	
	private int port;
	
	public NettyClient(String host, int port) {
		this.host = host;
		this.port = port;
		this.remoteClient = new Bootstrap();
		this.remoteGroup = new NioEventLoopGroup();
		this.isFinish = false;
		this.isClosed = true;
	}

	public void connection() {
		this.remoteClient.group(remoteGroup).channel(NioSocketChannel.class);
		this.remoteClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,5000);
		this.remoteClient.option(ChannelOption.TCP_NODELAY, true);
		this.remoteClient.option(ChannelOption.SO_KEEPALIVE, true);
		this.remoteClient.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ChannelPipeline p = ch.pipeline();
				p.addLast(new OracleDecode());
				p.addLast(new ClientHandler(NettyClient.this,getOracleServerHandler()));
			}
		});
		
		this.remoteClient.connect(host, port).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()) {
					clientChannel = future.channel();
					isClosed = false;
					logger.info("Connection to " + host + ":" + port + " successfully.");
				}else {
					logger.info("Connection to " + host + ":" + port + " failed.");
					close();
				}
				isFinish = true;
				notifyTask();
			}
		});
	}
	
	private synchronized void notifyTask() {
		this.notifyAll();
	}
	
	public synchronized void waitConnect() throws InterruptedException {
		while(!isFinish) {
			wait();
		}
	}
	
	public OracleServerHandler getOracleServerHandler() {
		return oracleServerHandler;
	}

	public void setOracleServerHandler(OracleServerHandler oracleServerHandler) {
		this.oracleServerHandler = oracleServerHandler;
	}

	public void writeAndFlush(byte[] data) {
		if(this.clientChannel.isOpen()) {
			ByteBuf buff = clientChannel.alloc().buffer(data.length);
			buff.writeBytes(data);
			this.clientChannel.writeAndFlush(buff);
		}
	}
	
	public boolean isClosed() {
		return this.isClosed;
	}
	
	public void close() {
		this.isClosed = true;
		if(this.oracleServerHandler != null) {
			this.oracleServerHandler.close();
		}
		if(this.clientChannel != null && this.clientChannel.isOpen()) {
			this.clientChannel.close();
		}
		
		if(this.remoteGroup != null) {
			this.remoteGroup.shutdownGracefully();
		}
		logger.info("Disconnection to " + host + ":" + port + " .");
	}
}

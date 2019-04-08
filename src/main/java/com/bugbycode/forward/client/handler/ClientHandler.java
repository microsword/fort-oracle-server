package com.bugbycode.forward.client.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.forward.client.startup.NettyClient;
import com.bugbycode.forward.server.handler.OracleServerHandler;
import com.bugbycode.module.Message;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

	private final Logger logger = LogManager.getLogger(ClientHandler.class);
	
	private NettyClient client;
	
	private OracleServerHandler oracleServerHandler;
	
	public ClientHandler(NettyClient client,
			OracleServerHandler oracleServerHandler) {
		this.client = client;
		this.oracleServerHandler = oracleServerHandler;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		int len = msg.readableBytes();
		byte[] buff = new byte[len];
		msg.readBytes(buff);
		this.oracleServerHandler.send(new Message(buff));
	}
	
	@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception{
		super.channelInactive(ctx);
		this.client.close();
	}
	
	@Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception{
		logger.error(cause.getMessage());
		ctx.channel().close();
    }

}

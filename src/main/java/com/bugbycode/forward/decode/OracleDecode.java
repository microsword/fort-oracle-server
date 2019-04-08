package com.bugbycode.forward.decode;

import java.util.List;

import com.util.TransferUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class OracleDecode extends ByteToMessageDecoder {

	private boolean isOracle12c = false;
	
	private byte[] buff = {};
	
	private int offset = 0;
	
	private int len = -1;
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		try {
			int readLen = -1;
			int tmp_len = -1;
			byte[] tmp;
			while((readLen = in.readableBytes()) > 0) {
				tmp_len = len - offset;
				if(offset == 0) {
					tmp = new byte[0x04];
					in.readBytes(tmp);
					if(!isOracle12c) {
						isOracle12c = TransferUtil.isOracle12c(tmp);
					}
					len = TransferUtil.toHH(tmp, isOracle12c);
					buff = new byte[len];
					System.arraycopy(tmp, 0, buff, offset, tmp.length);
					offset += tmp.length;
				}else if(readLen <= tmp_len){
					tmp = new byte[readLen];
					in.readBytes(tmp);
					System.arraycopy(tmp, 0, buff, offset, readLen);
					offset += readLen;
				}else if(readLen > tmp_len){
					tmp = new byte[tmp_len];
					in.readBytes(tmp);   
					System.arraycopy(tmp, 0, buff, offset, tmp_len);
					offset += tmp_len;
				}
				if(offset == len) {
					offset = 0;
					ByteBuf buf = ctx.alloc().buffer(len);
					buf.writeBytes(buff);
					out.add(buf);
					buff = null;
				}
			}
			/*
			byte[] read_buf = new byte[readLen];
			in.readBytes(read_buf);
			
			if(offset == 0) {
				if(!isOracle12c) {
					isOracle12c = TransferUtil.isOracle12c(read_buf);
				}
				len = TransferUtil.toHH(read_buf,isOracle12c);
				buff = new byte[len];
			}
			
			System.arraycopy(read_buf, 0, buff, offset, readLen);
			
			offset += readLen;
			if(offset == buff.length) {
				offset = 0;
				ByteBuf buf = ctx.alloc().buffer(buff.length);
				buf.writeBytes(buff);
				out.add(buf);
				buff = null;
			}*/
		}catch (Exception e) {
			ctx.close();
			e.printStackTrace();
			throw e;
		}
	}

}

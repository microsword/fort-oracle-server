package com.bugbycode.forward.server.handler;

import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.config.AppConfig;
import com.bugbycode.forward.client.startup.NettyClient;
import com.bugbycode.module.ForwardOauth;
import com.bugbycode.module.Message;
import com.bugbycode.module.OracleServer;
import com.bugbycode.module.ResourceServer;
import com.bugbycode.service.oracle.OracleHostFormatService;
import com.bugbycode.service.oracle.impl.OracleHostFormatServiceImpl;
import com.util.StringUtil;
import com.util.TransferUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import oracle.security.o5logon.BugByCodeO5Logon;

public class OracleServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

	private final Logger logger = LogManager.getLogger(OracleServerHandler.class);
	
	private ForwardOauth forwardOauth;
	
	private ResourceServer resourceServer;
	
	private OracleHostFormatService oracleHostFormatService;
	
	private boolean isClosed;
	
	private boolean isOracle12c;
	
	private OracleServer server;
	
	private Channel oracleServerChannel;
	
	private LinkedList<Message> queue;
	
	private NettyClient client;
	
	private String loginName;
	
	private String loginPassword;
	
	private byte[] AUTH_SESSKEY;
	
	private byte[] AUTH_PASSWORD;
	
	private byte[] encryptedSK;
	
	private byte[] AUTH_VFR_DATA;
	
	private byte[] custom_encryptedSK;
	
	private int verifierType;
	
	private byte[] AUTH_PBKDF2_CSK_SALT;
	
	private byte[] AUTH_PBKDF2_SPEEDY_KEY;
	
	private int AUTH_PBKDF2_VGEN_COUNT = -1;
	
	private int AUTH_PBKDF2_SDER_COUNT = -1;
	
	private byte[] AUTH_SVR_RESPONSE;
	
	private boolean isAuth;
	
	public OracleServerHandler(ForwardOauth forwardOauth,ResourceServer resourceServer) {
		this.oracleHostFormatService = new OracleHostFormatServiceImpl(forwardOauth,resourceServer);
		this.queue = new LinkedList<Message>();
		this.isClosed = true;
		this.isOracle12c = false;
		this.isAuth = false;
		this.forwardOauth = forwardOauth;
		this.resourceServer = resourceServer;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		int len = msg.readableBytes();
		byte[] buff = new byte[len];
		msg.readBytes(buff);
		if(!isOracle12c) {
			isOracle12c = TransferUtil.isOracle12c(buff);
		}
		logger.debug("0	" + new String(buff));
		logger.debug("0	" + StringUtil.byteToHexString(buff));
		if(TransferUtil.isConn(buff,isOracle12c)) {
			OracleServer server = oracleHostFormatService.format(buff);
			byte[] body_buff = server.getBody().getBytes();
			int index = TransferUtil.findKey(AppConfig.CONNECTION_KEY, buff);
			int conn_buff_len = index + body_buff.length;
			byte[] conn_buff = new byte[conn_buff_len];
			System.arraycopy(buff, 0, conn_buff, 0, index);
			System.arraycopy(body_buff, 0, conn_buff, index, body_buff.length);
			
			conn_buff[0x19] = (byte)((conn_buff[0x19] & 0xFF) + (conn_buff_len - buff.length));
			TransferUtil.toHH(conn_buff_len, conn_buff,isOracle12c);
			
			if(this.server == null) {
				this.server = server;
				this.client = new NettyClient(this.server.getOracleHost(), this.server.getOraclePort());
				this.client.setOracleServerHandler(this);
				this.client.connection();
				this.client.waitConnect();
				if(this.client.isClosed()) {
					throw new RuntimeException("Connection error.");
				}
				this.client.writeAndFlush(conn_buff);
			}else if(this.server.toString().equals(server.toString())){
				this.client.writeAndFlush(conn_buff);
			}else {
				ctx.close();
			}
		}else if(TransferUtil.isAuthRecv(buff,isOracle12c)){
			//将目标登录账号替换为目标设备账号
			loginName = TransferUtil.getUserName(AppConfig.AUTH_TERMINAL,buff);
			this.loginPassword = server.getPassword();
			logger.debug("Login user : " + loginName);
			byte[] login_name_buff = loginName.getBytes();
			byte[] account_buff = server.getOracleAccount().getBytes();
			byte[] tmp = TransferUtil.replaceAuthLoginName(login_name_buff, 
					account_buff, buff,isOracle12c);
			logger.debug("8	" + new String(tmp));
			logger.debug("8	" + StringUtil.byteToHexString(tmp));
			buff = tmp;
			
			this.client.writeAndFlush(buff);
		}else if(TransferUtil.isLogin(buff,isOracle12c)){
			//将目标登录账号替换为目标设备账号
			loginName = TransferUtil.getUserName(AppConfig.AUTH_SESSKEY,buff);
			AUTH_SESSKEY = TransferUtil.findKeyText(AppConfig.AUTH_SESSKEY.getBytes(), buff);
			AUTH_PASSWORD = TransferUtil.findKeyText(AppConfig.AUTH_PASSWORD.getBytes(), buff);
			AUTH_PBKDF2_SPEEDY_KEY = TransferUtil.findKeyText(AppConfig.AUTH_PBKDF2_SPEEDY_KEY.getBytes(), buff);
			logger.debug("AUTH_SESSKEY:" + new String(AUTH_SESSKEY));
			logger.debug("AUTH_PASSWORD:" + new String(AUTH_PASSWORD));
			boolean useDes = false;
			if(verifierType == 18453) {
				useDes = true;
			}
			BugByCodeO5Logon logon = new BugByCodeO5Logon(useDes);
			this.isAuth = logon.auth(loginName, loginPassword, new String(AUTH_PASSWORD), new String(custom_encryptedSK), 
					new String(AUTH_SESSKEY), verifierType, AUTH_VFR_DATA, AUTH_PBKDF2_SPEEDY_KEY, 
					AUTH_PBKDF2_CSK_SALT, AUTH_PBKDF2_VGEN_COUNT, AUTH_PBKDF2_SDER_COUNT, (byte)0, false);
			logger.debug("Login auth : " + this.isAuth);
			if(!this.isAuth) {
				throw new RuntimeException("Username or password error.");
			}
			
			this.AUTH_SVR_RESPONSE = logon.getEncryptedResponse(loginName, loginPassword, 
					new String(AUTH_PASSWORD), new String(custom_encryptedSK), new String(AUTH_SESSKEY), verifierType, 
					AUTH_VFR_DATA, AUTH_PBKDF2_CSK_SALT, AUTH_PBKDF2_VGEN_COUNT, AUTH_PBKDF2_SDER_COUNT, (byte)0,false);
			//10G 11G
			/*byte[] var8 = new byte[256];
			int[] var9 = new int[1];
			byte[] var15 = new byte[256];
			int[] var16 = new int[1];
			*/
			//10g 11g 12C
			int[] var41 = new int[1];
			byte[] var27 = new byte[256];
			
			int[] var34 = new int[1];
			byte[] var23 = new byte[256];
			int[] var24 = new int[1];
			byte[] var25 = new byte[256];
			
			byte[] encryptedKB = new byte[encryptedSK.length];
			
			String oraclePassword = server.getOraclePassword();
			/*
			logon.generateOAuthResponse(verifierType, AUTH_VFR_DATA, server.getOracleAccount(), oraclePassword, 
					oraclePassword.getBytes(), encryptedSK, encryptedKB, var8, var9, 
					false, (byte)0, AUTH_PBKDF2_CSK_SALT, 
					AUTH_PBKDF2_VGEN_COUNT, AUTH_PBKDF2_SDER_COUNT, var15, var16);
			
			byte[] tmp = new byte[var9[0]];
			System.arraycopy(var8, 0, tmp, 0, tmp.length);
			var8 = tmp;
			*/
			logon.generateOAuthResponse(verifierType, AUTH_VFR_DATA, server.getOracleAccount(), oraclePassword, oraclePassword, 
					oraclePassword.getBytes(), oraclePassword.getBytes(), encryptedSK, encryptedKB,
					var23, var27, var34, 
					var41, false, (byte)0, 
					AUTH_PBKDF2_CSK_SALT, AUTH_PBKDF2_VGEN_COUNT, AUTH_PBKDF2_SDER_COUNT, var25, var24);
			
			byte[] tmp = new byte[var34[0]];
			System.arraycopy(var23, 0, tmp, 0, tmp.length);
			byte[] var8 = tmp;
			
			tmp = new byte[var41[0]];
			System.arraycopy(var27, 0, tmp, 0, tmp.length);
			byte[] new_pwd = tmp;
			
			byte[] auth_pbkdf2_speedy_key = new byte[var24[0]];
			System.arraycopy(var25, 0, auth_pbkdf2_speedy_key, 0, auth_pbkdf2_speedy_key.length);
			
			logger.debug("NEW_AUTH_PASSWOR : " + new String(var8));
			logger.debug("NEW_AUTH_NEWPASSWOR : " + new String(new_pwd));
			logger.debug("NEW_AUTH_SESSKEY : " + new String(encryptedKB));
			logger.debug("NEW_AUTH_PBKDF2_SPEEDY_KEY : " + new String(auth_pbkdf2_speedy_key));
			
			int index = TransferUtil.findKey(AUTH_SESSKEY, buff);
			System.arraycopy(encryptedKB, 0, buff, index, encryptedKB.length);
			
			index = TransferUtil.findKey(AUTH_PASSWORD, buff);
			System.arraycopy(var8, 0, buff, index, var8.length);
			
			if(AUTH_PBKDF2_SPEEDY_KEY.length > 0) {
				index = TransferUtil.findKey(AUTH_PBKDF2_SPEEDY_KEY, buff);
				System.arraycopy(auth_pbkdf2_speedy_key, 0, buff, index, auth_pbkdf2_speedy_key.length);
			}
			
			logger.debug("Login user : " + loginName);
			byte[] login_name_buff = loginName.getBytes();
			byte[] account_buff = server.getOracleAccount().getBytes();
			tmp = TransferUtil.replaceAuthLoginName(login_name_buff, account_buff, buff,isOracle12c);
			buff = tmp;
			
			this.client.writeAndFlush(buff);
		}else {
			this.client.writeAndFlush(buff);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		//关闭连接
		super.channelInactive(ctx);
		if(this.client != null) {
			this.client.close();
		}
		this.isClosed = true;
		notifyTask();
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		//客户端连接
		super.channelActive(ctx);
		this.oracleServerChannel = ctx.channel();
		this.isClosed = false;
		new WorkThread().start();
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error(cause.getMessage());
		this.oracleServerChannel.close();
	}
	
	private void writeAndFlush(ByteBuf buff) {
		this.oracleServerChannel.writeAndFlush(buff);
	}
	
	public Channel getOracleServerChannel() {
		return this.oracleServerChannel;
	}
	
	public synchronized void send(Message msg) {
		this.queue.addLast(msg);
		this.notifyTask();
	}
	
	private synchronized void notifyTask() {
		this.notifyAll();
	}
	
	private synchronized byte[] read() throws InterruptedException {
		while(queue.isEmpty()) {
			wait();
			if(isClosed) {
				throw new InterruptedException("User client exit.");
			}
		}
		
		Message msg = queue.removeFirst();
		
		return msg.getData();
	}
	
	public synchronized void close() {
		this.oracleServerChannel.close();
		this.notifyTask();
	}
	
	private class WorkThread extends Thread{

		@Override
		public void run() {
			while(!isClosed) {
				try {
					byte[] data = read();
					logger.debug("1	" + new String(data));
					logger.debug("1	" + StringUtil.byteToHexString(data));
					if(TransferUtil.contailsEncryptedSK(data,isOracle12c)) {
						AUTH_VFR_DATA = TransferUtil.findSalt(data);
						encryptedSK = TransferUtil.findKeyText(AppConfig.AUTH_SESSKEY.getBytes(), data);
						verifierType = TransferUtil.getVerifierType(data);
						int offset = TransferUtil.findKey(AppConfig.AUTH_PBKDF2_CSK_SALT.getBytes(), data);
						if(offset != -1) {
							AUTH_PBKDF2_CSK_SALT = TransferUtil.findKeyText(AppConfig.AUTH_PBKDF2_CSK_SALT.getBytes(), data);
						}
						offset = TransferUtil.findKey(AppConfig.AUTH_PBKDF2_VGEN_COUNT.getBytes(), data);
						if(offset != -1) {
							byte[] vgenCountByte = TransferUtil.findKeyText(AppConfig.AUTH_PBKDF2_VGEN_COUNT.getBytes(), data);
							AUTH_PBKDF2_VGEN_COUNT = Integer.valueOf(new String(vgenCountByte));
						}
						offset = TransferUtil.findKey(AppConfig.AUTH_PBKDF2_SDER_COUNT.getBytes(), data);
						if(offset != -1) {
							byte[] sderCountByte = TransferUtil.findKeyText(AppConfig.AUTH_PBKDF2_SDER_COUNT.getBytes(), data);
							AUTH_PBKDF2_SDER_COUNT = Integer.valueOf(new String(sderCountByte));
						}
						logger.debug("AUTH_VFR_DATA:" + new String(AUTH_VFR_DATA));
						logger.debug("encryptedSK:" + new String(encryptedSK));
						logger.debug("verifierType:" + verifierType);
						if(AUTH_PBKDF2_CSK_SALT != null) {
							logger.debug("AUTH_PBKDF2_CSK_SALT:" + new String(AUTH_PBKDF2_CSK_SALT));
						}
						logger.debug("AUTH_PBKDF2_VGEN_COUNT:" + AUTH_PBKDF2_VGEN_COUNT);
						logger.debug("AUTH_PBKDF2_SDER_COUNT:" + AUTH_PBKDF2_SDER_COUNT);
						boolean useDes = false;
						if(verifierType == 18453) {
							useDes = true;
						}
						BugByCodeO5Logon logon = new BugByCodeO5Logon(useDes);
						try {
							custom_encryptedSK = logon.getEncryptedSK(verifierType, AUTH_VFR_DATA, loginName, loginPassword, AUTH_PBKDF2_VGEN_COUNT, (byte)0, false);
							if(custom_encryptedSK.length != encryptedSK.length) {
								throw new InterruptedException("EncryptedSK error.");
							}
							int index = TransferUtil.findKey(encryptedSK, data);
							System.arraycopy(custom_encryptedSK, 0, data, index, custom_encryptedSK.length);
						} catch (Exception e) {
							e.printStackTrace();
							throw new InterruptedException(e.getMessage());
						}
					}else if(TransferUtil.contailsResponse(data,isOracle12c)) {
						byte[] old_response = TransferUtil.findKeyText(AppConfig.AUTH_SVR_RESPONSE.getBytes(), data);
						logger.debug("old_response : " + new String(old_response));
						int index = TransferUtil.findKey(old_response, data);
						System.arraycopy(AUTH_SVR_RESPONSE, 0, data, index, AUTH_SVR_RESPONSE.length);
					}
					
					ByteBuf buf = oracleServerChannel.alloc().buffer(data.length);
					buf.writeBytes(data);
					writeAndFlush(buf);
				} catch (InterruptedException e) {
					logger.error(e.getLocalizedMessage());
				}
			}
		}
		
	}
}

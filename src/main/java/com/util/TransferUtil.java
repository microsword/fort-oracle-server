package com.util;

import com.bugbycode.config.AppConfig;

public class TransferUtil {
	
	public static boolean isOracle12c(byte[] buff) {
		return toHH(buff) == 0;
	}
	
	public static int toHH(byte[] buff) {
		return ((buff[0] << 0x08) & 0xFF00) | (buff[1] & 0xFF);
	}
	
	public static int toHH(byte[] buff,boolean isOracle12c) {
		int len = 0;
		if(isOracle12c) {
			len = toHH4Byte(buff);
		}else {
			len = toHH(buff);
		}
		return len;
	}
	
	public static int toHH4Byte(byte[] buff) {
		return ((buff[0] << 0x18) & 0xFF000000) | ((buff[1] << 0x10) & 0xFF0000) | ((buff[2] << 0x08) & 0xFF00) | (buff[3] & 0xFF);
	}
	
	public static void toHH(int len,byte[] buff) {
		buff[0] = (byte)((len >> 0x08) & 0xFF);
		buff[1] = (byte)(len & 0xFF);
	}
	
	public static void toHH4Byte(int len,byte[] buff) {
		buff[0] = (byte)((len >> 0x18) & 0xFF);
		buff[1] = (byte)((len >> 0x10) & 0xFF);
		buff[2] = (byte)((len >> 0x08) & 0xFF);
		buff[3] = (byte)(len & 0xFF);
	}
	
	public static void toHH(int len,byte[] buff,boolean isOracle12c) {
		if(isOracle12c) {
			toHH4Byte(len, buff);
		}else {
			toHH(len, buff);
		}
	}
	
	public static boolean isConn(byte[] buff,boolean isOracle12c) {
		return toHH(buff,isOracle12c) > 0x0A && buff[0x08] == 0x01 && buff[0x09] == 0x3D;
	}
	
	public static boolean isAuthRecv(byte[] buff,boolean isOracle12c) {
		return toHH(buff,isOracle12c) > 0xb && buff[0x0A] == 0x03 && buff[0x0B] == 0x76;
	}
	
	public static boolean isLogin(byte[] buff,boolean isOracle12c) {
		return toHH(buff,isOracle12c) > 0xb && buff[0x0A] == 0x03 && buff[0x0B] == 0x73;
	}
	
	public static boolean contailsEncryptedSK(byte[] buff,boolean isOracle12c) {
		return toHH(buff,isOracle12c) > 0xb && buff[0x0A] == 0x08 && findKey(AppConfig.AUTH_SESSKEY.getBytes(), buff) != -1;
	}
	
	public static boolean contailsResponse(byte[] buff,boolean isOracle12c) {
		return toHH(buff,isOracle12c) > 10 && TransferUtil.findKey(AppConfig.AUTH_SVR_RESPONSE.getBytes(), buff) != -1;
	}
	
	public static boolean isAuthEncryptedSK(byte[] buff,boolean isOracle12c) {
		return toHH(buff,isOracle12c) > 0x0A && buff[0x0A] == 0x08 && 
				(buff[0x0B] == 0x02 || buff[0x0B] == 0x03) 
				&& findKey(AppConfig.AUTH_SESSKEY.getBytes(),buff) != -1;
	}
	
	public static boolean isRecvAuthVersion(byte[] buff,boolean isOracle12c) {
		return toHH(buff,isOracle12c) > 0xb && buff[0x0A] == 0x08 && buff[0x0B] == 0x23;
	}
	
	public static boolean isPasswordError(byte[] buff,boolean isOracle12c) {
		return toHH(buff,isOracle12c) == 0xb && buff[0x04] == 0x0c;
	}
	
	public static byte[] getErrorDescription(boolean isOracle12c) {
		byte[] tmp = isOracle12c ? AppConfig.PASSWORD_ERROR_DESCRIPTION_12C : AppConfig.PASSWORD_ERROR_DESCRIPTION;
		byte[] buff = new byte[tmp.length];
		System.arraycopy(tmp, 0, buff, 0, buff.length);
		return buff;
	}
	
	public static String getUserName(String key,byte[] buff) {
		int user_index = TransferUtil.findKey(key.getBytes(), buff) - 0x06;
		int user_len = -1;
		while(user_index > 0) {
			user_len = buff[user_index] & 0xFF;
			if(user_len < 0x21) {
				break;
			}
			user_index--;
		}
		
		byte[] userByte = new byte[user_len];
		System.arraycopy(buff, user_index + 1, userByte, 0, user_len);
		return new String(userByte);
	}
	
	public static int findKey(byte[] key,byte[] buff) {
		int keLen = key.length;
		int length = buff.length;
		for(int index = 0;index < length - keLen;index++) {
			int keyIndex = 0;
			while(keyIndex < keLen) {
				if(key[keyIndex] != buff[index + keyIndex]) {
					break;
				}
				keyIndex++;
			}
			if(keyIndex == keLen) {
				return index;
			}
		}
		return -1;
	}
	
	public static byte[] findKeyText(byte[] key,byte[] buff) {
		int index = findKey(key, buff);
		if(index == -1) {
			return new byte[0];
		}
		index += key.length;
		index += 0x04;
		int keyTextLen = buff[index++] & 0xFF;
		byte[] key_text = new byte[keyTextLen];
		System.arraycopy(buff, index, key_text, 0, keyTextLen);
		return key_text;
	}
	
	public static byte[] findSalt(byte[] buff) {
		int index = TransferUtil.findKey(AppConfig.AUTH_VFR_DATA.getBytes(), buff);
		if(index == -1) {
			return new byte[0];
		}
		index += AppConfig.AUTH_VFR_DATA.length();
		int num = readSh4(index, buff);
		if(num == 0) {
			return new byte[0];
		}
		index += 0x04;
		int len = buff[index++] & 0xFF;
		byte[] tmp = new byte[len];
		System.arraycopy(buff, index, tmp, 0, len);
		return tmp;
	}
	
	public static int getVerifierType(byte[] buff) {
		int index = TransferUtil.findKey(AppConfig.AUTH_VFR_DATA.getBytes(), buff);
		if(index == -1) {
			return 2361;
		}
		index += AppConfig.AUTH_VFR_DATA.length();
		int num = readSh4(index, buff);
		index += 0x04;
		if(num == 0) {
			return formatVerifierType(index,buff);
		}
		int len = buff[index++] & 0xFF;
		index += len;
		return formatVerifierType(index,buff);
	}
	
	public static int formatVerifierType(int index,byte[] buff) {
		return (buff[index++] & 0xFF) | ((buff[index] & 0xFF) << 0x08);
	}
	
	public static int readSh4(int index,byte[] buff) {
		return ((buff[index++] & 0xFF) << 0x18) | ((buff[index++] & 0xFF) << 0x10)
				| ((buff[index++] & 0xFF) << 0x08) | (buff[index] & 0xFF);
	}
	
	public static byte[] replaceAuthLoginName(byte[] loginName,byte[] oracleAccount,byte[] buff,boolean isOracle12c) {
		int sub = oracleAccount.length - loginName.length;
		int index = findKey(loginName, buff);
		byte[] head = new byte[index];
		System.arraycopy(buff, 0, head, 0, head.length);
		byte[] end = new byte[buff.length - (index + loginName.length)];
		System.arraycopy(buff, index + loginName.length, end, 0, end.length);
		byte[] tmp = new byte[head.length + oracleAccount.length + end.length];
		System.arraycopy(head, 0, tmp, 0, head.length);
		System.arraycopy(oracleAccount, 0, tmp, head.length, oracleAccount.length);
		System.arraycopy(end, 0, tmp, head.length + oracleAccount.length, end.length);
		int len = toHH(tmp,isOracle12c) + sub;
		toHH(len, tmp, isOracle12c);
		index = findKey(oracleAccount, tmp);
		tmp[index - 1] = (byte)(oracleAccount.length & 0xFF);
		
		index = 0x0d;
		if((tmp[index] & 0xFF) == 0xFE) {
			index += 0x08;
		}else {
			index++;
		}
		if((tmp[index] & 0xFF) == loginName.length) {
			tmp[index] = (byte)(oracleAccount.length & 0xFF);
		}else if((tmp[index] & 0xFF) == (loginName.length * 2)) {
			tmp[index] = (byte)((oracleAccount.length * 2) & 0xFF);
		}
		
		return tmp;
	}
}

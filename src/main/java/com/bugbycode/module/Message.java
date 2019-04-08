package com.bugbycode.module;

import java.io.Serializable;

public class Message implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3941384719522111268L;
	
	private byte[] buff;
	
	public Message(byte[] buff) {
		this.buff = buff;
	}
	
	public byte[] getData() {
		return this.buff;
	}
}

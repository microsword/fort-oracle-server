package com.bugbycode.service.oracle;

import java.io.IOException;

import com.bugbycode.module.OracleServer;

public interface OracleHostFormatService {
	
	public OracleServer format(byte[] buff) throws IOException;
}

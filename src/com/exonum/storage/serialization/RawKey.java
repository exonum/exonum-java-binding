package com.exonum.storage.serialization;

public class RawKey {
	
	private final byte[] key;
	
	public RawKey(byte[] raw) {
		this.key = raw;
	}
	
	public byte[] getRaw() {
		return key;
	}
}

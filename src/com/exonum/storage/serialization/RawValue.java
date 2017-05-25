package com.exonum.storage.serialization;

public class RawValue {

	private final byte[] value;
	
	public RawValue(byte[] raw) {
		this.value = raw;
	}
	
	public byte[] getRaw() {
		return value;
	}
}

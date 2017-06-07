package com.exonum.storage.serialization;

public interface StorageValue {

	public RawValue serializeToRaw();	
	public void deserializeFromRaw(byte[] raw);
}

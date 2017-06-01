package com.exonum.storage.serialization;

public interface StorageValue {

	public RawValue serializeToRaw();	
	public StorageValue deserializeFromRaw(byte[] raw);	
}

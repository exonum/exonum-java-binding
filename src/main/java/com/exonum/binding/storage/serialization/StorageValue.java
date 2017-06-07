package com.exonum.binding.storage.serialization;

public interface StorageValue {

	public RawValue serializeToRaw();	
	public void deserializeFromRaw(byte[] raw);
}

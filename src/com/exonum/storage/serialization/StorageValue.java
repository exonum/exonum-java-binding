package com.exonum.storage.serialization;

//import com.exonum.storage.exception.NotImplementedMethodException;

public interface StorageValue {

	public RawValue serializeToRaw();
	
	public StorageValue deserializeFromRaw(byte[] raw);
	
//	public static StorageValue deserializeFromRaw(byte[] raw) throws NotImplementedMethodException{
//		throw new NotImplementedMethodException();
//	}
}

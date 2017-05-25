package com.exonum.index;

import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.exonum.storage.connector.Connect;
import com.exonum.storage.exception.StorageOperationException;
import com.exonum.storage.serialization.RawValue;
import com.exonum.storage.serialization.StorageKey;
import com.exonum.storage.serialization.StorageValue;


public class IndexMap<K extends StorageKey, V extends StorageValue> {

	private final Class<? extends StorageValue> valueClass;
	private final Connect dbConnect;
	private final Object nativeIndexMap;
	
	public IndexMap(Class<? extends StorageValue> valueClass, Connect connect, byte[] prefix){
		
		this.valueClass = valueClass;
		this.dbConnect = connect;
		this.nativeIndexMap = createNativeIndexMap(connect, prefix);		
	}
	
	public void put(K key, V value) {
		
		dbConnect.lockWrite();
		try {
			if(!putToIndexMap(key.serializeToRaw().getRaw(), value.serializeToRaw().getRaw(), nativeIndexMap)){				
				throw new StorageOperationException();
			}
		} finally {
			dbConnect.unlockWrite();
		}
	}
	
	public StorageValue get(K key) {
		
		dbConnect.lockRead();
		RawValue rawWrapper = null;
		try {
			rawWrapper = new RawValue(getFromIndexMap(key.serializeToRaw().getRaw(), nativeIndexMap));
		} finally {
			dbConnect.unlockRead();
		}
		
		// temporary deserialization decision
		V tmp = null;
		try {
			tmp = (V)valueClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		return tmp.deserializeFromRaw(rawWrapper.getRaw());
	}
	
	public void delete(K key) {
		
		dbConnect.lockRead();
		try {
			if(!deleteFromIndexMap(key.serializeToRaw().getRaw(), nativeIndexMap)){				
				throw new StorageOperationException();
			}
		} finally {
			dbConnect.unlockRead();
		}
	}
	
	private native boolean putToIndexMap(byte[] key, byte[] value, Object nativeIndex);
	private native byte[] getFromIndexMap(byte[] key, Object nativeIndex);
	private native boolean deleteFromIndexMap(byte[] key, Object nativeIndex);
	private native Object createNativeIndexMap(Connect connect, byte[] prefix);
}

package com.exonum.test.main;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import com.exonum.index.IndexMap;
import com.exonum.storage.exception.StorageOperationException;
import com.exonum.storage.serialization.RawKey;
import com.exonum.storage.serialization.RawValue;
import com.exonum.storage.serialization.StorageKey;
import com.exonum.storage.serialization.StorageValue;

public class MainTest {

	public static void main(String[] args) {
		
		StorageKey key  = new StorageKey() {
			
			@Override
			public RawKey serializeToRaw() {
				
				ByteArrayOutputStream bos = null;
				ObjectOutput out = null;
				try{
					bos = new ByteArrayOutputStream();
			        out = new ObjectOutputStream(bos);
			        out.writeObject(this);
				} catch (IOException e) {
					// TODO: handle exception
				}
		        
		        return new RawKey(bos.toByteArray());
			}
		};
		
		StorageValue value  = new StorageValue() {
			
			@Override
			public RawValue serializeToRaw() {
				
				ByteArrayOutputStream bos = null;
				ObjectOutput out = null;
				try{
					bos = new ByteArrayOutputStream();
			        out = new ObjectOutputStream(bos);
			        out.writeObject(this);
				} catch (IOException e) {
					// TODO: handle exception
				}
		        
		        return new RawValue(bos.toByteArray());
			}
		};
		
		IndexMap<StorageKey, StorageValue> map = new IndexMap<StorageKey, StorageValue>(value.getClass(), 1, null);
		
		try {
			map.put(key, value);
		} catch (StorageOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		StorageValue valueFromMap = null;
		try {
			valueFromMap = map.get(key);
		} catch (ClassNotFoundException | IOException | StorageOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

}

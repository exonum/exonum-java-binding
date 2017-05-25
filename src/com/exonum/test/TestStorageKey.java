package com.exonum.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import com.exonum.storage.serialization.RawKey;
import com.exonum.storage.serialization.StorageKey;

public class TestStorageKey implements StorageKey {

	public int key = 1;
	
	@Override
	public RawKey serializeToRaw() {
		
		ByteArrayOutputStream bos = null;
		ObjectOutput out = null;
		byte[] rawResult = null;
		try{
			bos = new ByteArrayOutputStream();
	        out = new ObjectOutputStream(bos);
	        out.writeObject(this);
	        rawResult = bos.toByteArray();
	        out.close();
	        bos.close();
		} catch (IOException e) {
			// TODO: handle exception
		}
        
        return new RawKey(rawResult);
	}
}

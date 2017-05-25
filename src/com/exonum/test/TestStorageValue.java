package com.exonum.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import com.exonum.storage.serialization.RawValue;
import com.exonum.storage.serialization.StorageValue;

public class TestStorageValue implements StorageValue {

	public String value = "Store me";
	
	@Override
	public RawValue serializeToRaw() {
		
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
        
        return new RawValue(rawResult);
	}

	@Override
	public StorageValue deserializeFromRaw(byte[] raw) {
		
	    ByteArrayInputStream bis = new ByteArrayInputStream(raw);
	    ObjectInputStream in = null;
	    StorageValue res = null;
		try {
			in = new ObjectInputStream(bis);
			res = (StorageValue)in.readObject();
		} catch (IOException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return res;
	}

}

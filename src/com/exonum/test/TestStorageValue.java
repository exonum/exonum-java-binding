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
        byte[] rawResult = toBytes();
        return new RawValue(rawResult);
    }

    private byte[] toBytes() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeChars(value);
            return bos.toByteArray();
        } catch (IOException e) {
            // ignored, as byte output stream implementation does not throw.
            return new byte[0];
        }
    }

	@Override
	public void deserializeFromRaw(byte[] raw) {
	    try (ByteArrayInputStream bis = new ByteArrayInputStream(raw);
	    	 ObjectInputStream in = new ObjectInputStream(bis)) {
	    	this.value = (String)in.readObject();
		} catch (IOException | ClassNotFoundException e) {
			// temporary test decision
		}

	}
}

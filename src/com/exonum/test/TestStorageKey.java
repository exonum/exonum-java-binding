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
        byte[] rawResult = toBytes();
        return new RawKey(rawResult);
    }

    private byte[] toBytes() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeInt(key);
            return bos.toByteArray();
        } catch (IOException e) {
            // ignored, as byte output stream implementation does not throw.
            return new byte[0];
        }
    }
}

package com.exonum.binding.index;

import com.exonum.binding.storage.serialization.RawKey;
import com.exonum.binding.storage.serialization.StorageKey;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

class TestStorageKey implements StorageKey {

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
      // Must be unreachable, as byte output stream implementation does not throw.
      throw new AssertionError(e);
    }
  }
}

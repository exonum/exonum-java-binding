package com.exonum.binding.index;

import com.exonum.binding.storage.serialization.RawValue;
import com.exonum.binding.storage.serialization.StorageValue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Objects;

class TestStorageValue implements StorageValue {

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
      // Must be unreachable, as byte output stream implementation does not throw.
      throw new AssertionError(e);
    }
  }

  @Override
  public void deserializeFromRaw(byte[] raw) {
    try (ByteArrayInputStream bis = new ByteArrayInputStream(raw);
        ObjectInputStream in = new ObjectInputStream(bis)) {
      this.value = (String) in.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TestStorageValue that = (TestStorageValue) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}

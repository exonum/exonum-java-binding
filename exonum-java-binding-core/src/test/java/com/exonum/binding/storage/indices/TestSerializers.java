package com.exonum.binding.storage.indices;

import com.exonum.binding.storage.serialization.Serializer;
import java.nio.charset.StandardCharsets;

class TestSerializers {

  static Serializer<String> string() {
    return StringSerializer.INSTANCE;
  }

  static Serializer<byte[]> bytes() {
    return NoopSerializer.INSTANCE;
  }

  enum StringSerializer implements Serializer<String> {
    INSTANCE;

    @Override
    public byte[] toBytes(String value) {
      return value.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String fromBytes(byte[] serializedValue) {
      return new String(serializedValue, StandardCharsets.UTF_8);
    }
  }

  enum NoopSerializer implements Serializer<byte[]> {
    INSTANCE;

    @Override
    public byte[] toBytes(byte[] value) {
      return value;
    }

    @Override
    public byte[] fromBytes(byte[] serializedValue) {
      return serializedValue;
    }
  }

  private TestSerializers() {}
}

package com.exonum.binding.cryptocurrency;

import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.storage.serialization.Serializer;

public enum PublicKeySerializer implements Serializer<PublicKey> {
  INSTANCE;

  @Override
  public byte[] toBytes(PublicKey value) {
    return value.toBytes();
  }

  @Override
  public PublicKey fromBytes(byte[] serializedValue) {
    return PublicKey.fromBytes(serializedValue);
  }
}

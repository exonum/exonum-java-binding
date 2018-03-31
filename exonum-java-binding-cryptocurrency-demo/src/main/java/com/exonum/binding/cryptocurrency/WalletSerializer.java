package com.exonum.binding.cryptocurrency;

import com.exonum.binding.storage.serialization.Serializer;

import java.io.*;

public enum WalletSerializer implements Serializer<Wallet> {
  INSTANCE;

  @Override
  public byte[] toBytes(Wallet value) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ObjectOutputStream os = new ObjectOutputStream(out);
      os.writeObject(value);
      return out.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Serialization error: " + e.getMessage());
    }
  }

  @Override
  public Wallet fromBytes(byte[] serializedValue) {
    try {
      ByteArrayInputStream in = new ByteArrayInputStream(serializedValue);
      ObjectInputStream is = new ObjectInputStream(in);
      return (Wallet) is.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException("Deserialization error: " + e.getMessage());
    }
  }
}

package com.exonum.binding.cryptocurrency;

import com.exonum.binding.storage.serialization.Serializer;
import com.google.protobuf.InvalidProtocolBufferException;

public enum WalletSerializer implements Serializer<Wallet> {
  INSTANCE;

  @Override
  public byte[] toBytes(Wallet value) {
    WalletProtos.Wallet wallet = WalletProtos.Wallet.newBuilder()
        .setBalance(value.getBalance())
        .build();
    return wallet.toByteArray();
  }

  @Override
  public Wallet fromBytes(byte[] binaryWallet) {
    Wallet wallet;
    try {
      WalletProtos.Wallet copiedWalletProtos = WalletProtos.Wallet.parseFrom(binaryWallet);
      wallet = new Wallet(copiedWalletProtos.getBalance());
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(
          "Unable to instantiate WalletProtos.Wallet instance from provided binary data", e);
    }
    return wallet;
  }
}

package com.exonum.binding.cryptocurrency;

import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.storage.serialization.Serializer;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum WalletSerializer implements Serializer<Wallet> {
  INSTANCE;

  private static final Logger logger = LogManager.getLogger(WalletSerializer.class);

  @Override
  public byte[] toBytes(Wallet value) {
    WalletProtos.PublicKey publicKey = WalletProtos.PublicKey.newBuilder()
        .setRawKey(ByteString.copyFrom(value.getPublicKey().toBytes()))
        .build();
    WalletProtos.Wallet wallet = WalletProtos.Wallet.newBuilder()
        .setPublicKey(publicKey)
        .setBalance(value.getBalance())
        .build();
    return wallet.toByteArray();
  }

  @Override
  public Wallet fromBytes(final byte[] binaryWallet)
  {
    Wallet wallet = null;
    try {
      WalletProtos.Wallet copiedWalletProtos = WalletProtos.Wallet.parseFrom(binaryWallet);
      PublicKey publicKey = PublicKey.fromBytes(copiedWalletProtos.getPublicKey().toByteArray());
      wallet = new Wallet(publicKey, copiedWalletProtos.getBalance());
    }
    catch (InvalidProtocolBufferException e)
    {
      logger.error(
          "Unable to instantiate WalletProtos.Wallet instance from provided binary data", e);
    }
    return wallet;
  }
}

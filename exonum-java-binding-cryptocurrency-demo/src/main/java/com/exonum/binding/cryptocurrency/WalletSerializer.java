package com.exonum.binding.cryptocurrency;

import com.exonum.binding.storage.serialization.Serializer;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum WalletSerializer implements Serializer<Wallet> {
  INSTANCE;

  private static final Logger logger = LogManager.getLogger(WalletSerializer.class);

  @Override
  public byte[] toBytes(Wallet value) {
    WalletProtos.Wallet wallet = WalletProtos.Wallet.newBuilder()
        .setName(value.getName())
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
      wallet = new Wallet(copiedWalletProtos.getName(), copiedWalletProtos.getBalance());
    }
    catch (InvalidProtocolBufferException e)
    {
      logger.error(
          "Unable to instantiate WalletProtos.Wallet instance from provided binary data", e);
    }
    return wallet;
  }
}

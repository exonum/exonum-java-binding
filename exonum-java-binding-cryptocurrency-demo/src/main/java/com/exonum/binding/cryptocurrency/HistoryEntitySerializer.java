package com.exonum.binding.cryptocurrency;

import static com.google.protobuf.ByteString.copyFrom;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.cryptocurrency.transactions.TxMessagesProtos;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public enum HistoryEntitySerializer implements Serializer<HistoryEntity> {
  INSTANCE;

  @Override
  public byte[] toBytes(HistoryEntity value) {
    TxMessagesProtos.HistoryEntity entity = TxMessagesProtos.HistoryEntity.newBuilder()
        .setSeed(value.getSeed())
        .setWalletFrom(keyToByte(value.getWalletFrom()))
        .setWalletTo(keyToByte(value.getWalletTo()))
        .setSum(value.getAmount())
        .setHash(copyFrom(value.getTransactionHash().asBytes()))
        .build();

    return entity.toByteArray();

  }

  @Override
  public HistoryEntity fromBytes(byte[] serializedValue) {
    try {
      TxMessagesProtos.HistoryEntity entity = TxMessagesProtos.HistoryEntity
          .parseFrom(serializedValue);

      return HistoryEntity.Builder.newBuilder()
          .setSeed(entity.getSeed())
          .setWalletFrom(bytesToKey(entity.getWalletFrom()))
          .setWalletTo(bytesToKey(entity.getWalletTo()))
          .setAmount(entity.getSum())
          .setTransactionHash(HashCode.fromBytes(entity.getHash().toByteArray()))
          .build();
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(
          "Unable to instantiate TxMessagesProtos.TransferTx instance from provided binary data",
          e);
    }
  }

  private static ByteString keyToByte(PublicKey key) {
    return copyFrom(key.toBytes());
  }

  private static PublicKey bytesToKey(ByteString bytes) {
    return PublicKey.fromBytes(bytes.toByteArray());
  }
}

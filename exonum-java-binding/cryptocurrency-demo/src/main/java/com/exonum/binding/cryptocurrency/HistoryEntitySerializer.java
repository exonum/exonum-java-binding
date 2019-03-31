package com.exonum.binding.cryptocurrency;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.cryptocurrency.transactions.TxMessageProtos;
import com.google.protobuf.ByteString;

import static com.exonum.binding.common.serialization.StandardSerializers.protobuf;

public enum HistoryEntitySerializer implements Serializer<HistoryEntity> {
  INSTANCE;

  private static final Serializer<TxMessageProtos.HistoryEntity> PROTO_SERIALIZER =
      protobuf(TxMessageProtos.HistoryEntity.class);

  @Override
  public byte[] toBytes(HistoryEntity value) {
    TxMessageProtos.HistoryEntity HistoryEntity = TxMessageProtos.HistoryEntity.newBuilder()
        .setSeed(value.getSeed())
        .setWalletFrom(fromPublicKey(value.getWalletFrom()))
        .setWalletTo(fromPublicKey(value.getWalletTo()))
        .setSum(value.getAmount())
        .setTxMessageHash(ByteString.copyFrom(value.getTxMessageHash().asBytes()))
        .build();

    return HistoryEntity.toByteArray();
  }

  @Override
  public HistoryEntity fromBytes(byte[] serializedHistoryEntity) {
    TxMessageProtos.HistoryEntity copiedHistoryEntityProtos =
        PROTO_SERIALIZER.fromBytes(serializedHistoryEntity);
    return HistoryEntity.newBuilder()
        .setSeed(copiedHistoryEntityProtos.getSeed())
        .setWalletFrom(Wallet.toPublicKey(copiedHistoryEntityProtos.getWalletFrom()))
        .setWalletTo(Wallet.toPublicKey(copiedHistoryEntityProtos.getWalletTo()))
        .setAmount(copiedHistoryEntityProtos.getSum())
        .setTxMessageHash(HashCode.fromBytes(copiedHistoryEntityProtos.getTxMessageHash().toByteArray()))
        .build();
  }

  private static ByteString fromPublicKey(PublicKey k) {
    return ByteString.copyFrom(k.toBytes());
  }
}

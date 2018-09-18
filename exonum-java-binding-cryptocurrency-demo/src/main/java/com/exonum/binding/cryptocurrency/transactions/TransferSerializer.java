package com.exonum.binding.cryptocurrency.transactions;

import static com.google.protobuf.ByteString.copyFrom;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.serialization.Serializer;
import com.google.protobuf.InvalidProtocolBufferException;

public enum TransferSerializer implements Serializer<TransferTxData> {
  INSTANCE;

  @Override
  public byte[] toBytes(TransferTxData value) {
    TxMessagesProtos.TransferTx transferTx =
        TxMessagesProtos.TransferTx.newBuilder()
            .setSeed(value.getSeed())
            .setFromWallet(copyFrom(value.getSenderId().toBytes()))
            .setToWallet(copyFrom(value.getRecipientId().toBytes()))
            .setSum(value.getAmount())
            .build();
    return transferTx.toByteArray();
  }

  @Override
  public TransferTxData fromBytes(byte[] serializedValue) {
    try {
      TxMessagesProtos.TransferTx tx =
          TxMessagesProtos.TransferTx.parseFrom(serializedValue);

      return new TransferTxData(tx.getSeed(),
          PublicKey.fromBytes(tx.getFromWallet().toByteArray()),
          PublicKey.fromBytes(tx.getToWallet().toByteArray()),
          tx.getSum());
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(
          "Unable to instantiate TxMessagesProtos.TransferTx instance from provided binary data",
          e);

    }

  }
}

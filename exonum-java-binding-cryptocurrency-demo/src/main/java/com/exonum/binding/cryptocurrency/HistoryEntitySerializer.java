/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.cryptocurrency;

import static com.exonum.binding.common.serialization.StandardSerializers.protobuf;
import static com.google.protobuf.ByteString.copyFrom;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.cryptocurrency.transactions.TxMessageProtos;
import com.google.protobuf.ByteString;

public enum HistoryEntitySerializer implements Serializer<HistoryEntity> {
  INSTANCE;

  private static final Serializer<TxMessageProtos.HistoryEntity> historyEntityProtobufSerializer =
      protobuf(TxMessageProtos.HistoryEntity.class);

  @Override
  public byte[] toBytes(HistoryEntity value) {
    TxMessageProtos.HistoryEntity entity = TxMessageProtos.HistoryEntity.newBuilder()
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
    TxMessageProtos.HistoryEntity entity =
        historyEntityProtobufSerializer.fromBytes(serializedValue);

    return HistoryEntity.Builder.newBuilder()
        .setSeed(entity.getSeed())
        .setWalletFrom(bytesToKey(entity.getWalletFrom()))
        .setWalletTo(bytesToKey(entity.getWalletTo()))
        .setAmount(entity.getSum())
        .setTransactionHash(HashCode.fromBytes(entity.getHash().toByteArray()))
        .build();
  }

  private static ByteString keyToByte(PublicKey key) {
    return copyFrom(key.toBytes());
  }

  private static PublicKey bytesToKey(ByteString bytes) {
    return PublicKey.fromBytes(bytes.toByteArray());
  }

}

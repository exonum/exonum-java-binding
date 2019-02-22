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

package com.exonum.binding.blockchain.serialization;

import static com.exonum.binding.common.serialization.StandardSerializers.protobuf;

import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.serialization.Serializer;

public enum TransactionLocationSerializer implements Serializer<TransactionLocation> {
  INSTANCE;

  private static final Serializer<CoreProtos.TxLocation> PROTO_SERIALIZER =
      protobuf(CoreProtos.TxLocation.class);

  @Override
  public byte[] toBytes(TransactionLocation value) {
    CoreProtos.TxLocation txLocation =
        CoreProtos.TxLocation.newBuilder()
            .setBlockHeight(value.getHeight())
            .setPositionInBlock(value.getIndexInBlock())
            .build();
    return txLocation.toByteArray();
  }

  @Override
  public TransactionLocation fromBytes(byte[] binaryTransactionLocation) {
    CoreProtos.TxLocation copiedtxLocationProtos =
        PROTO_SERIALIZER.fromBytes(binaryTransactionLocation);
    return TransactionLocation.valueOf(copiedtxLocationProtos.getBlockHeight(),
        copiedtxLocationProtos.getPositionInBlock());
  }

}

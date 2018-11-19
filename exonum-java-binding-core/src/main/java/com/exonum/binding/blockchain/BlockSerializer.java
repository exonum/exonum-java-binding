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

package com.exonum.binding.blockchain;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.Serializer;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

enum BlockSerializer implements Serializer<Block> {
  INSTANCE;

  @Override
  public byte[] toBytes(Block value) {
    BlockProtos.Block block = BlockProtos.Block.newBuilder()
        .setProposerId(value.getProposerId())
        .setHeight(value.getHeight())
        .setNumTransactions(value.getNumTransactions())
        .setPreviousBlockHash(toByteString(value.getPreviousBlockHash()))
        .setTxRootHash(toByteString(value.getTxRootHash()))
        .setStateHash(toByteString(value.getStateHash()))
        .build();
    return block.toByteArray();
  }

  @Override
  public Block fromBytes(byte[] binaryBlock) {
    Block block;
    try {
      BlockProtos.Block copiedBlockProtos = BlockProtos.Block.parseFrom(binaryBlock);
      block = new Block((short) copiedBlockProtos.getProposerId(),
          copiedBlockProtos.getHeight(), copiedBlockProtos.getNumTransactions(),
          toByteString(copiedBlockProtos.getPreviousBlockHash()),
          toByteString(copiedBlockProtos.getTxRootHash()),
          toByteString(copiedBlockProtos.getStateHash()));
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(
          "Unable to instantiate BlockProtos.Block instance from provided binary data", e);
    }
    return block;
  }

  private static ByteString toByteString(HashCode hash) {
    return ByteString.copyFrom(hash.asBytes());
  }

  private static HashCode toByteString(ByteString byteString) {
    return HashCode.fromBytes(byteString.toByteArray());
  }

}

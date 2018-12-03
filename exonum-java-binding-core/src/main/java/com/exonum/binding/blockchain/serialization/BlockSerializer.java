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

import com.exonum.binding.blockchain.Block;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public enum BlockSerializer implements Serializer<Block> {
  INSTANCE;

  @Override
  public byte[] toBytes(Block value) {
    BlockProtos.Block block = BlockProtos.Block.newBuilder()
        .setProposerId(value.getProposerId())
        .setHeight(value.getHeight())
        .setTxCount(value.getNumTransactions())
        .setPrevHash(toHashProto(value.getPreviousBlockHash()))
        .setTxHash(toHashProto(value.getTxRootHash()))
        .setStateHash(toHashProto(value.getStateHash()))
        .build();
    return block.toByteArray();
  }

  @Override
  public Block fromBytes(byte[] binaryBlock) {
    try {
      BlockProtos.Block copiedBlocks = BlockProtos.Block.parseFrom(binaryBlock);
      return Block.valueOf(copiedBlocks.getProposerId(),
          copiedBlocks.getHeight(), copiedBlocks.getTxCount(),
          fromHashProto(copiedBlocks.getPrevHash()),
          fromHashProto(copiedBlocks.getTxHash()),
          fromHashProto(copiedBlocks.getStateHash()));
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(
          "Unable to instantiate Blocks.Block instance from provided binary data", e);
    }
  }

  private static BlockProtos.Hash toHashProto(HashCode hash) {
    return BlockProtos.Hash.newBuilder()
        .setData(toByteString(hash))
        .build();
  }

  private static HashCode fromHashProto(BlockProtos.Hash hash) {
    return toHashCode(hash.getData());
  }

  private static ByteString toByteString(HashCode hash) {
    byte[] bytes = StandardSerializers.hash().toBytes(hash);
    return ByteString.copyFrom(bytes);
  }

  private static HashCode toHashCode(ByteString byteString) {
    byte[] bytes = byteString.toByteArray();
    return StandardSerializers.hash().fromBytes(bytes);
  }

}

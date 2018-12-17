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

import com.exonum.binding.blockchain.Block;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.serialization.Serializer;
import com.google.protobuf.ByteString;

public enum BlockSerializer implements Serializer<Block> {
  INSTANCE;

  private static final Serializer<CoreProtos.Block> PROTO_SERIALIZER =
      protobuf(CoreProtos.Block.class);

  @Override
  public byte[] toBytes(Block value) {
    CoreProtos.Block block = CoreProtos.Block.newBuilder()
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
    HashCode blockHash = Hashing.sha256().hashBytes(binaryBlock);
    CoreProtos.Block copiedBlocks = PROTO_SERIALIZER.fromBytes(binaryBlock);
    return Block.builder()
        .proposerId(copiedBlocks.getProposerId())
        .height(copiedBlocks.getHeight())
        .numTransactions(copiedBlocks.getTxCount())
        .blockHash(blockHash)
        .previousBlockHash(fromHashProto(copiedBlocks.getPrevHash()))
        .txRootHash(fromHashProto(copiedBlocks.getTxHash()))
        .stateHash(fromHashProto(copiedBlocks.getStateHash()))
        .build();
  }

  private static CoreProtos.Hash toHashProto(HashCode hash) {
    return CoreProtos.Hash.newBuilder()
        .setData(toByteString(hash))
        .build();
  }

  private static HashCode fromHashProto(CoreProtos.Hash hash) {
    return toHashCode(hash.getData());
  }

  private static ByteString toByteString(HashCode hash) {
    byte[] bytes = hash.asBytes();
    return ByteString.copyFrom(bytes);
  }

  private static HashCode toHashCode(ByteString byteString) {
    byte[] bytes = byteString.toByteArray();
    return HashCode.fromBytes(bytes);
  }

}

/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.core.blockchain.serialization;

import static com.exonum.binding.common.hash.Hashing.sha256;
import static com.exonum.binding.common.serialization.StandardSerializers.protobuf;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.core.blockchain.Block;
import com.exonum.core.messages.Blockchain;
import com.exonum.core.messages.Blockchain.AdditionalHeaders;
import com.exonum.core.messages.KeyValueSequenceOuterClass.KeyValue;
import com.exonum.core.messages.KeyValueSequenceOuterClass.KeyValueSequence;
import com.exonum.core.messages.KeyValueSequenceOuterClass.KeyValueSequence.Builder;
import com.exonum.core.messages.Types;
import com.exonum.core.messages.Types.Hash;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;

public enum BlockSerializer implements Serializer<Block> {
  INSTANCE;

  private static final Serializer<Blockchain.Block> PROTO_SERIALIZER =
      protobuf(Blockchain.Block.class);

  @Override
  public byte[] toBytes(Block value) {
    Blockchain.Block block =
        Blockchain.Block.newBuilder()
            .setProposerId(value.getProposerId())
            .setHeight(value.getHeight())
            .setTxCount(value.getNumTransactions())
            .setPrevHash(toHashProto(value.getPreviousBlockHash()))
            .setTxHash(toHashProto(value.getTxRootHash()))
            .setStateHash(toHashProto(value.getStateHash()))
            .setErrorHash(toHashProto(value.getErrorHash()))
            .setAdditionalHeaders(toHeadersProto(value.getAdditionalHeaders()))
            .build();
    return block.toByteArray();
  }

  @Override
  public Block fromBytes(byte[] binaryBlock) {
    Blockchain.Block blockMessage = PROTO_SERIALIZER.fromBytes(binaryBlock);
    HashCode blockHash = sha256().hashBytes(binaryBlock);
    return newBlockInternal(blockMessage, blockHash);
  }

  /**
   * Creates a block from a message and the block hash. Does not check the hash correctness — hence
   * for internal usage only.
   */
  public static Block newBlockInternal(Blockchain.Block blockMessage, HashCode blockHash) {
    return Block.builder()
        .proposerId(blockMessage.getProposerId())
        .height(blockMessage.getHeight())
        .numTransactions(blockMessage.getTxCount())
        .blockHash(blockHash)
        .previousBlockHash(toHashCode(blockMessage.getPrevHash()))
        .txRootHash(toHashCode(blockMessage.getTxHash()))
        .stateHash(toHashCode(blockMessage.getStateHash()))
        .errorHash(toHashCode(blockMessage.getErrorHash()))
        .additionalHeaders(toHeadersMap(blockMessage.getAdditionalHeaders()))
        .build();
  }

  private static Types.Hash toHashProto(HashCode hash) {
    ByteString bytes = ByteString.copyFrom(hash.asBytes());
    return Types.Hash.newBuilder().setData(bytes).build();
  }

  private static HashCode toHashCode(Hash hash) {
    ByteString bytes = hash.getData();
    return HashCode.fromBytes(bytes.toByteArray());
  }

  @VisibleForTesting
  static ImmutableMap<String, ByteString> toHeadersMap(AdditionalHeaders headers) {
    return headers.getHeaders().getEntryList().stream()
        .collect(toImmutableMap(KeyValue::getKey, KeyValue::getValue));
  }

  @VisibleForTesting
  static AdditionalHeaders toHeadersProto(ImmutableMap<String, ByteString> headers) {
    Builder additionalHeadersBuilder = KeyValueSequence.newBuilder();

    headers.forEach((k, v) -> additionalHeadersBuilder.addEntry(toProtoEntry(k, v)));

    return AdditionalHeaders.newBuilder().setHeaders(additionalHeadersBuilder.build()).build();
  }

  private static KeyValue toProtoEntry(String key, ByteString value) {
    return KeyValue.newBuilder().setKey(key).setValue(value).build();
  }
}

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

import static com.exonum.binding.common.serialization.StandardSerializers.protobuf;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.core.blockchain.Block;
import com.exonum.core.messages.Blockchain;
import com.exonum.core.messages.Blockchain.AdditionalHeaders;
import com.exonum.core.messages.Types;
import com.exonum.core.messages.Types.Hash;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import exonum.KeyValueSequenceOuterClass.KeyValue;
import exonum.KeyValueSequenceOuterClass.KeyValueSequence;
import java.util.LinkedHashMap;
import java.util.Optional;

public enum BlockSerializer implements Serializer<Block> {
  INSTANCE;

  private static final Serializer<Blockchain.Block> PROTO_SERIALIZER =
      protobuf(Blockchain.Block.class);

  @Override
  public byte[] toBytes(Block value) {
    Blockchain.Block block = Blockchain.Block.newBuilder()
        .setProposerId(value.getProposerId())
        .setHeight(value.getHeight())
        .setTxCount(value.getNumTransactions())
        .setPrevHash(toHashProto(value.getPreviousBlockHash()))
        .setTxHash(toHashProto(value.getTxRootHash()))
        .setStateHash(toHashProto(value.getStateHash()))
        .setErrorHash(toHashProtoOptional(value.getErrorHash()))
        .setAdditionalHeaders(toHeadersProto(value.getAdditionalHeaders()))
        .build();
    return block.toByteArray();
  }

  @Override
  public Block fromBytes(byte[] binaryBlock) {
    HashCode blockHash = Hashing.sha256().hashBytes(binaryBlock);
    Blockchain.Block copiedBlocks = PROTO_SERIALIZER.fromBytes(binaryBlock);
    return Block.builder()
        .proposerId(copiedBlocks.getProposerId())
        .height(copiedBlocks.getHeight())
        .numTransactions(copiedBlocks.getTxCount())
        .blockHash(blockHash)
        .previousBlockHash(toHashCode(copiedBlocks.getPrevHash()))
        .txRootHash(toHashCode(copiedBlocks.getTxHash()))
        .stateHash(toHashCode(copiedBlocks.getStateHash()))
        .errorHash(toOptionalHashCode(copiedBlocks.getErrorHash()))
        .additionalHeaders(toHeadersMap(copiedBlocks.getAdditionalHeaders()))
        .build();
  }

  private static Types.Hash toHashProto(HashCode hash) {
    ByteString bytes = ByteString.copyFrom(hash.asBytes());
    return Types.Hash.newBuilder()
        .setData(bytes)
        .build();
  }

  private static HashCode toHashCode(Hash hash) {
    ByteString bytes = hash.getData();
    return HashCode.fromBytes(bytes.toByteArray());
  }

  private static Optional<HashCode> toOptionalHashCode(Hash hash) {
    return Optional.of(hash)
        .map(Hash::getData)
        .filter(h -> !h.isEmpty())
        .map(ByteString::toByteArray)
        .map(HashCode::fromBytes);
  }

  private static Types.Hash toHashProtoOptional(Optional<HashCode> hash) {
    ByteString bytes = hash
        .map(HashCode::asBytes)
        .map(ByteString::copyFrom)
        .orElse(ByteString.EMPTY);

    return Types.Hash.newBuilder()
        .setData(bytes)
        .build();
  }

  @VisibleForTesting
  static ImmutableMap<String, ByteString> toHeadersMap(AdditionalHeaders headers) {
    return headers.getHeaders().getEntryList()
        .stream()
        .collect(collectingAndThen(
            toMap(
                KeyValue::getKey,
                KeyValue::getValue,
                (o, n) -> {
                  throw new IllegalStateException(String.format(
                      "Should never happen. Duplicate key found in headers %s", headers));
                },
                LinkedHashMap::new),
            ImmutableMap::copyOf)
        );
  }

  @VisibleForTesting
  static AdditionalHeaders toHeadersProto(ImmutableMap<String, ByteString> headers) {
    return AdditionalHeaders.newBuilder()
        .setHeaders(
            KeyValueSequence.newBuilder()
                .addAllEntry(
                    headers.entrySet()
                        .stream()
                        .map(e -> KeyValue.newBuilder()
                            .setKey(e.getKey())
                            .setValue(e.getValue())
                            .build())
                        .collect(toList())
                )
                .build()
        )
        .build();
  }

}

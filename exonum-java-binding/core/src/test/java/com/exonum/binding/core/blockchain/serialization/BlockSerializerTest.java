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

import static com.exonum.binding.core.blockchain.serialization.BlockSerializer.toHeadersMap;
import static com.exonum.binding.core.blockchain.serialization.BlockSerializer.toHeadersProto;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.core.blockchain.Block;
import com.exonum.binding.core.blockchain.Blocks;
import com.exonum.core.messages.Blockchain;
import com.exonum.core.messages.Blockchain.AdditionalHeaders;
import com.exonum.core.messages.KeyValueSequenceOuterClass.KeyValue;
import com.exonum.core.messages.KeyValueSequenceOuterClass.KeyValueSequence;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class BlockSerializerTest {

  private static final Serializer<Block> BLOCK_SERIALIZER = BlockSerializer.INSTANCE;

  @ParameterizedTest
  @MethodSource("testSource")
  void roundTrip(Block expected) {
    byte[] bytes = BLOCK_SERIALIZER.toBytes(expected);
    Block actual = BLOCK_SERIALIZER.fromBytes(bytes);

    assertThat(actual, equalTo(expected));
  }

  @ParameterizedTest
  @MethodSource("testSource")
  void roundTripFromMessage(Block expected) throws InvalidProtocolBufferException {
    byte[] asBytes = BLOCK_SERIALIZER.toBytes(expected);
    Blockchain.Block asMessage = Blockchain.Block.parseFrom(asBytes);
    Block actual = Block.fromMessage(asMessage);

    assertThat(actual, equalTo(expected));
  }

  private static Stream<Block> testSource() {
    Block block1 = Block.builder()
        .proposerId(0)
        .height(1)
        .numTransactions(2)
        .blockHash(HashCode.fromString("ab"))
        .previousBlockHash(HashCode.fromString("bc"))
        .txRootHash(HashCode.fromString("cd"))
        .stateHash(HashCode.fromString("ab"))
        .additionalHeaders(ImmutableMap.of())
        .errorHash(HashCode.fromString("ef"))
        .build();
    Block block2 = Block.builder()
        .proposerId(Integer.MAX_VALUE)
        .height(Long.MAX_VALUE)
        .numTransactions(Integer.MAX_VALUE)
        .blockHash(HashCode.fromString("ab"))
        .previousBlockHash(HashCode.fromString("bc"))
        .txRootHash(HashCode.fromString("cd"))
        .stateHash(HashCode.fromString("ab"))
        .additionalHeaders(ImmutableMap.of("one", ByteString.copyFromUtf8("abcd01")))
        .errorHash(HashCode.fromString("ef"))
        .build();

    return Stream.of(block1, block2)
        .map(Blocks::withProperHash);
  }

  @Test
  void headersMapStrictOrderRoundTripTest() {
    KeyValue first = KeyValue.newBuilder()
        .setKey("foo")
        .setValue(ByteString.EMPTY)
        .build();
    KeyValue second = KeyValue.newBuilder()
        .setKey("bar")
        .setValue(ByteString.EMPTY)
        .build();
    AdditionalHeaders expected = AdditionalHeaders.newBuilder()
        .setHeaders(KeyValueSequence.newBuilder()
            .addEntries(first)
            .addEntries(second)
            .build())
        .build();

    AdditionalHeaders actual = toHeadersProto(toHeadersMap(expected));

    assertThat(actual, is(expected));
  }

}

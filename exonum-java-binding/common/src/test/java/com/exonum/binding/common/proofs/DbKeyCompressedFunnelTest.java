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

package com.exonum.binding.common.proofs;

import static com.exonum.binding.common.proofs.map.DbKeyTestUtils.keyFromString;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.exonum.binding.common.hash.PrimitiveSink;
import com.exonum.binding.common.proofs.map.DbKey;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.Mockito;

class DbKeyCompressedFunnelTest {

  @ParameterizedTest
  @MethodSource("testSource")
  void funnelTest(DbKey dbKey, byte[] encodedSignificantBitsNum, int expectedWholeBytesKeyLength) {
    PrimitiveSink primitiveSink = mock(PrimitiveSink.class);

    DbKeyCompressedFunnel.dbKeyCompressedFunnel().funnel(dbKey, primitiveSink);

    InOrder inOrder = inOrder(primitiveSink);
    for (byte encodedByte : encodedSignificantBitsNum) {
      inOrder.verify(primitiveSink).putByte(encodedByte);
    }

    byte[] key = dbKey.getKeySlice();
    inOrder.verify(primitiveSink).putBytes(key, 0, expectedWholeBytesKeyLength);
    Mockito.verifyNoMoreInteractions(primitiveSink);
  }

  private static Stream<Arguments> testSource() {
    return Stream.of(
        arguments(DbKey.newBranchKey(keyFromString(""), 0b0_0000000),
            new byte[]{0b0_0000000},
            0),
        arguments(DbKey.newBranchKey(keyFromString("1"), 0b0_0000001),
            new byte[]{0b0_0000001},
            1),
        arguments(DbKey.newBranchKey(keyFromString("111"), 0b0_0000111),
            new byte[]{0b0_0000111},
            1),
        arguments(DbKey.newBranchKey(keyFromString("0001"), 0b0_0001000),
            new byte[]{0b0_0001000},
            1),
        arguments(DbKey.newBranchKey(keyFromString("1111"), 0b0_1111111),
            new byte[]{0b0_1111111},
            16),
        arguments(DbKey.newBranchKey(keyFromString("1001 1001"), 0b1_0000000),
            new byte[]{(byte) 0b1_0000000, 0b0_0000001},
            16),
        arguments(DbKey.newLeafKey(keyFromString("1111 1111")),
            new byte[]{(byte) 0b1_0000000, 0b0_0000010},
            32));
  }
}

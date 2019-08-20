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

import static com.exonum.binding.common.proofs.DbKeyCompressedFunnel.getWholeBytesKeyLength;
import static com.exonum.binding.common.proofs.DbKeyCompressedFunnel.writeUnsignedLeb128;
import static com.exonum.binding.common.proofs.map.DbKeyTestUtils.keyFromString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.exonum.binding.common.hash.PrimitiveSink;
import com.exonum.binding.common.proofs.map.DbKey;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DbKeyCompressedFunnelTest {

  @ParameterizedTest
  @MethodSource("testSource")
  void funnelTest(DbKey dbKey) {
    PrimitiveSink primitiveSink = mock(PrimitiveSink.class);
    DbKeyCompressedFunnel.dbKeyCompressedFunnel().funnel(dbKey, primitiveSink);
    int wholeBytesKeyLength = getWholeBytesKeyLength(dbKey.getNumSignificantBits());
    byte[] encodedSignificantBitsNum = new byte[3];
    int bytesWritten = writeUnsignedLeb128(encodedSignificantBitsNum,
        dbKey.getNumSignificantBits());
    byte[] key = dbKey.getKeySlice();

    verify(primitiveSink).putBytes(encodedSignificantBitsNum, 0, bytesWritten);
    verify(primitiveSink).putBytes(key, 0, wholeBytesKeyLength);
  }

  private static Stream<DbKey> testSource() {
    return Stream.of(
        DbKey.newBranchKey(keyFromString(""), 0),
        DbKey.newBranchKey(keyFromString("1"), 1),
        DbKey.newBranchKey(keyFromString("1001"), 255),
        DbKey.newLeafKey(keyFromString("1111 1111")));
  }
}

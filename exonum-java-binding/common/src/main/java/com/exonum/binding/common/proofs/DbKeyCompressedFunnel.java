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

import com.exonum.binding.common.hash.Funnel;
import com.exonum.binding.common.hash.PrimitiveSink;
import com.exonum.binding.common.proofs.map.DbKey;

/**
 * A funnel for a database key. Puts the LEB128 compressed binary representation of the given
 * database key into the sink.
 */
public enum DbKeyCompressedFunnel implements Funnel<DbKey> {
  INSTANCE;

  @Override
  public void funnel(DbKey from, PrimitiveSink into) {
    int bitsLength = from.getNumSignificantBits();
    // Perform division, rounding the result up
    int wholeBytesLength = (bitsLength + Byte.SIZE - 1) / Byte.SIZE;
    byte[] key = from.getKeySlice();

    writeUnsignedLeb128(into, bitsLength);
    into.putBytes(key, 0, wholeBytesLength);
  }

  public static Funnel<DbKey> dbKeyCompressedFunnel() {
    return INSTANCE;
  }

  private static void writeUnsignedLeb128(PrimitiveSink into, int value) {
    int remaining = value >>> 7;
    while (remaining != 0) {
      into.putByte((byte) ((value & 0x7f) | 0x80));
      value = remaining;
      remaining >>>= 7;
    }

    into.putByte((byte) (value & 0x7f));
  }
}

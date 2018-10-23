/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.common.hash;

import static com.google.common.io.BaseEncoding.base16;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.BaseEncoding;
import com.google.common.testing.ClassSanityTester;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HashCode}.
 *
 * @author Dimitris Andreou
 * @author Kurt Alfred Kluever
 */
class HashCodeTest {
  // note: asInt(), asLong() are in little endian
  private static final ImmutableList<ExpectedHashCode> expectedHashCodes =
      ImmutableList.of(
          new ExpectedHashCode(
              new byte[]{
                  (byte) 0xef, (byte) 0xcd, (byte) 0xab, (byte) 0x89,
                  (byte) 0x67, (byte) 0x45, (byte) 0x23, (byte) 0x01
              },
              0x89abcdef,
              0x0123456789abcdefL,
              "efcdab8967452301"),
          new ExpectedHashCode(
              new byte[]{
                  (byte) 0xef, (byte) 0xcd, (byte) 0xab, (byte) 0x89,
                  (byte) 0x67, (byte) 0x45, (byte) 0x23,
                  (byte) 0x01, // up to here, same bytes as above
                  (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
                  (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08
              },
              0x89abcdef,
              0x0123456789abcdefL, // asInt/asLong as above, due to equal eight first bytes
              "efcdab89674523010102030405060708"),
          new ExpectedHashCode(
              new byte[]{(byte) 0xdf, (byte) 0x9b, (byte) 0x57, (byte) 0x13},
              0x13579bdf,
              null,
              "df9b5713"),
          new ExpectedHashCode(
              new byte[]{(byte) 0xcd, (byte) 0xab, (byte) 0x00, (byte) 0x00},
              0x0000abcd,
              null,
              "cdab0000"),
          new ExpectedHashCode(
              new byte[]{
                  (byte) 0xef, (byte) 0xcd, (byte) 0xab, (byte) 0x00,
                  (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
              },
              0x00abcdef,
              0x0000000000abcdefL,
              "efcdab0000000000"));

  // expectedHashCodes must contain at least one hash code with 4 bytes
  @Test
  void testFromInt() {
    for (ExpectedHashCode expected : expectedHashCodes) {
      if (expected.bytes.length == 4) {
        HashCode fromInt = HashCode.fromInt(expected.asInt);
        assertExpectedHashCode(expected, fromInt);
      }
    }
  }

  // expectedHashCodes must contain at least one hash code with 8 bytes
  @Test
  void testFromLong() {
    for (ExpectedHashCode expected : expectedHashCodes) {
      if (expected.bytes.length == 8) {
        HashCode fromLong = HashCode.fromLong(expected.asLong);
        assertExpectedHashCode(expected, fromLong);
      }
    }
  }

  @Test
  void testFromBytes() {
    for (ExpectedHashCode expected : expectedHashCodes) {
      HashCode fromBytes = HashCode.fromBytes(expected.bytes);
      assertExpectedHashCode(expected, fromBytes);
    }
  }

  @Test
  void testFromBytes_copyOccurs() {
    byte[] bytes = new byte[]{(byte) 0xcd, (byte) 0xab, (byte) 0x00, (byte) 0x00};
    HashCode hashCode = HashCode.fromBytes(bytes);
    int expectedInt = 0x0000abcd;
    String expectedToString = "cdab0000";

    assertEquals(expectedInt, hashCode.asInt());
    assertEquals(expectedToString, hashCode.toString());

    bytes[0] = (byte) 0x00;

    assertEquals(expectedInt, hashCode.asInt());
    assertEquals(expectedToString, hashCode.toString());
  }

  @Test
  void testFromBytesNoCopy_noCopyOccurs() {
    byte[] bytes = new byte[]{(byte) 0xcd, (byte) 0xab, (byte) 0x00, (byte) 0x00};
    HashCode hashCode = HashCode.fromBytesNoCopy(bytes);

    assertEquals(0x0000abcd, hashCode.asInt());
    assertEquals("cdab0000", hashCode.toString());

    bytes[0] = (byte) 0x00;

    assertEquals(0x0000ab00, hashCode.asInt());
    assertEquals("00ab0000", hashCode.toString());
  }

  @Test
  void testGetBytesInternal_noCloneOccurs() {
    byte[] bytes = new byte[]{(byte) 0xcd, (byte) 0xab, (byte) 0x00, (byte) 0x00};
    HashCode hashCode = HashCode.fromBytes(bytes);

    assertEquals(0x0000abcd, hashCode.asInt());
    assertEquals("cdab0000", hashCode.toString());

    hashCode.getBytesInternal()[0] = (byte) 0x00;

    assertEquals(0x0000ab00, hashCode.asInt());
    assertEquals("00ab0000", hashCode.toString());
  }

  @Test
  void testPadToLong() {
    assertEquals(0x1111111111111111L, HashCode.fromLong(0x1111111111111111L).padToLong());
    assertEquals(0x9999999999999999L, HashCode.fromLong(0x9999999999999999L).padToLong());
    assertEquals(0x0000000011111111L, HashCode.fromInt(0x11111111).padToLong());
    assertEquals(0x0000000099999999L, HashCode.fromInt(0x99999999).padToLong());
  }

  @Test
  void testPadToLongWith4Bytes() {
    assertEquals(0x0000000099999999L, HashCode.fromBytesNoCopy(byteArrayWith9s(4)).padToLong());
  }

  @Test
  void testPadToLongWith6Bytes() {
    assertEquals(0x0000999999999999L, HashCode.fromBytesNoCopy(byteArrayWith9s(6)).padToLong());
  }

  @Test
  void testPadToLongWith8Bytes() {
    assertEquals(0x9999999999999999L, HashCode.fromBytesNoCopy(byteArrayWith9s(8)).padToLong());
  }

  private static byte[] byteArrayWith9s(int size) {
    byte[] bytez = new byte[size];
    Arrays.fill(bytez, (byte) 0x99);
    return bytez;
  }

  @Test
  void testToString() {
    byte[] data = new byte[]{127, -128, 5, -1, 14};
    assertEquals("7f8005ff0e", HashCode.fromBytes(data).toString());
    assertEquals("7f8005ff0e", base16().lowerCase().encode(data));
  }

  @Test
  void testHashCode_nulls() throws Exception {
    sanityTester().testNulls();
  }

  @Test
  void testHashCode_equalsAndSerializable() throws Exception {
    sanityTester().testEqualsAndSerializable();
  }

  @Test
  void testRoundTripHashCodeUsingBaseEncoding() {
    HashCode hash1 = Hashing.sha256().hashString("foo", Charsets.US_ASCII);
    HashCode hash2 = HashCode.fromBytes(BaseEncoding.base16().lowerCase().decode(hash1.toString()));
    assertEquals(hash1, hash2);
  }

  @Test
  void testObjectHashCode() {
    HashCode hashCode42 = HashCode.fromInt(42);
    assertEquals(42, hashCode42.hashCode());
  }

  // See https://code.google.com/p/guava-libraries/issues/detail?id=1494
  @Test
  void testObjectHashCodeWithSameLowOrderBytes() {
    // These will have the same first 4 bytes (all 0).
    byte[] bytesA = new byte[5];
    byte[] bytesB = new byte[5];

    // Change only the last (5th) byte
    bytesA[4] = (byte) 0xbe;
    bytesB[4] = (byte) 0xef;

    HashCode hashCodeA = HashCode.fromBytes(bytesA);
    HashCode hashCodeB = HashCode.fromBytes(bytesB);

    // They aren't equal...
    assertFalse(hashCodeA.equals(hashCodeB));

    // But they still have the same Object#hashCode() value.
    // Technically not a violation of the equals/hashCode contract, but...?
    assertEquals(hashCodeA.hashCode(), hashCodeB.hashCode());
  }

  @Test
  void testRoundTripHashCodeUsingFromString() {
    HashCode hash1 = Hashing.sha256().hashString("foo", Charsets.US_ASCII);
    HashCode hash2 = HashCode.fromString(hash1.toString());
    assertEquals(hash1, hash2);
  }

  @Test
  void testRoundTrip() {
    for (ExpectedHashCode expected : expectedHashCodes) {
      String string = HashCode.fromBytes(expected.bytes).toString();
      assertEquals(expected.toString, string);
      assertEquals(
          expected.toString,
          HashCode.fromBytes(BaseEncoding.base16().lowerCase().decode(string)).toString());
    }
  }

  @Test
  void testFromStringFailsWithInvalidHexChar() {
    assertThrows(IllegalArgumentException.class, () -> HashCode.fromString("7f8005ff0z"));
  }

  @Test
  void testFromStringFailsWithUpperCaseString() {
    String string = Hashing.sha256().hashString("foo", Charsets.US_ASCII).toString().toUpperCase();
    assertThrows(IllegalArgumentException.class, () -> HashCode.fromString(string));
  }

  @Test
  void testFromStringFailsWithShortInputs() {
    assertThrows(IllegalArgumentException.class, () -> HashCode.fromString(""));
    assertThrows(IllegalArgumentException.class, () -> HashCode.fromString("7"));
    HashCode unused = HashCode.fromString("7f");
  }

  @Test
  void testFromStringFailsWithOddLengthInput() {
    assertThrows(IllegalArgumentException.class, () -> HashCode.fromString("7f8"));
  }

  @Test
  void testIntWriteBytesTo() {
    byte[] dest = new byte[4];
    HashCode.fromInt(42).writeBytesTo(dest, 0, 4);
    assertTrue(Arrays.equals(HashCode.fromInt(42).asBytes(), dest));
  }

  @Test
  void testLongWriteBytesTo() {
    byte[] dest = new byte[8];
    HashCode.fromLong(42).writeBytesTo(dest, 0, 8);
    assertTrue(Arrays.equals(HashCode.fromLong(42).asBytes(), dest));
  }

  private static final HashCode HASH_ABCD =
      HashCode.fromBytes(new byte[]{(byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd});

  @Test
  void testWriteBytesTo() {
    byte[] dest = new byte[4];
    HASH_ABCD.writeBytesTo(dest, 0, 4);
    assertTrue(
        Arrays.equals(new byte[]{(byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd}, dest));
  }

  @Test
  void testWriteBytesToOversizedArray() {
    byte[] dest = new byte[5];
    HASH_ABCD.writeBytesTo(dest, 0, 4);
    assertTrue(
        Arrays.equals(
            new byte[]{(byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd, (byte) 0x00}, dest));
  }

  @Test
  void testWriteBytesToOversizedArrayLongMaxLength() {
    byte[] dest = new byte[5];
    HASH_ABCD.writeBytesTo(dest, 0, 5);
    assertTrue(
        Arrays.equals(
            new byte[]{(byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd, (byte) 0x00}, dest));
  }

  @Test
  void testWriteBytesToOversizedArrayShortMaxLength() {
    byte[] dest = new byte[5];
    HASH_ABCD.writeBytesTo(dest, 0, 3);
    assertTrue(
        Arrays.equals(
            new byte[]{(byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0x00, (byte) 0x00}, dest));
  }

  @Test
  void testWriteBytesToUndersizedArray() {
    byte[] dest = new byte[3];
    assertThrows(IndexOutOfBoundsException.class, () -> HASH_ABCD.writeBytesTo(dest, 0, 4));
  }

  @Test
  void testWriteBytesToUndersizedArrayLongMaxLength() {
    byte[] dest = new byte[3];
    assertThrows(IndexOutOfBoundsException.class, () -> HASH_ABCD.writeBytesTo(dest, 0, 5));
  }

  @Test
  void testWriteBytesToUndersizedArrayShortMaxLength() {
    byte[] dest = new byte[3];
    HASH_ABCD.writeBytesTo(dest, 0, 2);
    assertTrue(Arrays.equals(new byte[]{(byte) 0xaa, (byte) 0xbb, (byte) 0x00}, dest));
  }

  private static ClassSanityTester.FactoryMethodReturnValueTester sanityTester() {
    return new ClassSanityTester()
        .setDefault(byte[].class, new byte[]{1, 2, 3, 4})
        .setDistinctValues(byte[].class, new byte[]{1, 2, 3, 4}, new byte[]{5, 6, 7, 8})
        .setDistinctValues(String.class, "7f8005ff0e", "7f8005ff0f")
        .forAllPublicStaticMethods(HashCode.class);
  }

  private static void assertExpectedHashCode(ExpectedHashCode expectedHashCode, HashCode hash) {
    assertTrue(Arrays.equals(expectedHashCode.bytes, hash.asBytes()));
    byte[] bb = new byte[hash.bits() / 8];
    hash.writeBytesTo(bb, 0, bb.length);
    assertTrue(Arrays.equals(expectedHashCode.bytes, bb));
    assertEquals(expectedHashCode.asInt, hash.asInt());
    if (expectedHashCode.asLong == null) {
      assertThrows(IllegalStateException.class, () -> hash.asLong());
    } else {
      assertEquals(expectedHashCode.asLong.longValue(), hash.asLong());
    }
    assertEquals(expectedHashCode.toString, hash.toString());
    assertSideEffectFree(hash);
    assertReadableBytes(hash);
  }

  private static void assertSideEffectFree(HashCode hash) {
    byte[] original = hash.asBytes();
    byte[] mutated = hash.asBytes();
    mutated[0]++;
    assertTrue(Arrays.equals(original, hash.asBytes()));
  }

  private static void assertReadableBytes(HashCode hashCode) {
    assertTrue(hashCode.bits() >= 32); // sanity
    byte[] hashBytes = hashCode.asBytes();
    int totalBytes = hashCode.bits() / 8;

    for (int bytes = 0; bytes < totalBytes; bytes++) {
      byte[] bb = new byte[bytes];
      hashCode.writeBytesTo(bb, 0, bb.length);

      assertTrue(Arrays.equals(Arrays.copyOf(hashBytes, bytes), bb));
    }
  }

  private static class ExpectedHashCode {
    final byte[] bytes;
    final int asInt;
    final Long asLong; // null means that asLong should throw an exception
    final String toString;

    ExpectedHashCode(byte[] bytes, int asInt, Long asLong, String toString) {
      this.bytes = bytes;
      this.asInt = asInt;
      this.asLong = asLong;
      this.toString = toString;
    }
  }
}

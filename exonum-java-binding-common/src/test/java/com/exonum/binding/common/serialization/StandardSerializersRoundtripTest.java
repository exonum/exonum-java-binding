package com.exonum.binding.common.serialization;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.exonum.binding.common.hash.HashCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StandardSerializersRoundtripTest {

  @ParameterizedTest
  @ValueSource(longs = {Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE})
  void roundtripLongTest(Long value) {
    roundTripTest(value, StandardSerializers.longs());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "",
      "a",
      "δ", // A two-byte character
      "\uD83E\uDD37", // A four-byte character: a shrug emoji
      "ab",
      "cat",
      "Iñtërnâtiônàlizætiøn"})
  void roundtripStringTest(String value) {
    roundTripTest(value, StandardSerializers.string());
  }

  @ParameterizedTest
  @ValueSource(ints = {0x89abcdef, 0x13579bdf, 0x0000abcd, 0x0000abcdef})
  void roundtripHashCodeTest(int value) {
    roundTripTest(HashCode.fromInt(value), StandardSerializers.hash());
  }

  /**
   * Performs a round trip test: ObjectT -> Binary -> ObjectT.
   */
  private static <ObjectT, SerializerT extends Serializer<ObjectT>> void roundTripTest(
      ObjectT expected, SerializerT serializer) {
    byte[] bytes = serializer.toBytes(expected);
    ObjectT actual = serializer.fromBytes(bytes);

    assertThat(actual, equalTo(expected));
  }
}

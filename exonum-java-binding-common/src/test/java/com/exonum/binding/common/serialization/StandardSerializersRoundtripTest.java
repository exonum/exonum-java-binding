package com.exonum.binding.common.serialization;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.exonum.binding.common.hash.HashCode;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.jupiter.api.Test;

class StandardSerializersRoundtripTest {

  @Test
  void roundtripLongTest() {
    List<Long> valuesToTest = ImmutableList.of(
        Long.MIN_VALUE,
        0L,
        Long.MAX_VALUE
    );
    valuesToTest.forEach(v -> roundTripTest(v, StandardSerializers.longs()));
  }

  @Test
  void roundtripStringTest() {
    List<String> valuesToTest = ImmutableList.of(
        "",
        "a",
        "δ", // A two-byte character
        "\uD83E\uDD37", // A four-byte character: a shrug emoji
        "ab",
        "cat",
        "Iñtërnâtiônàlizætiøn"
    );
    valuesToTest.forEach(v -> roundTripTest(v, StandardSerializers.string()));
  }

  @Test
  void roundtripHashCodeTest() {
    List<HashCode> valuesToTest = ImmutableList.of(
        HashCode.fromInt(0x89abcdef),
        HashCode.fromInt(0x13579bdf),
        HashCode.fromInt(0x0000abcd),
        HashCode.fromInt(0x0000abcdef)
    );
    valuesToTest.forEach(v -> roundTripTest(v, StandardSerializers.hash()));
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

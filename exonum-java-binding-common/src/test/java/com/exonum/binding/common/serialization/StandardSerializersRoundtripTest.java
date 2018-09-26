package com.exonum.binding.common.serialization;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.google.common.collect.Streams;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
  @MethodSource("testHashes")
  void roundtripHashCodeTest(HashCode hashCode) {
    roundTripTest(hashCode, StandardSerializers.hash());
  }

  /**
   * Performs a round trip test: ObjectT -> Binary -> ObjectT.
   */
  static <ObjectT, SerializerT extends Serializer<ObjectT>> void roundTripTest(
      ObjectT expected, SerializerT serializer) {
    byte[] bytes = serializer.toBytes(expected);
    ObjectT actual = serializer.fromBytes(bytes);

    assertThat(actual, equalTo(expected));
  }

  private static Stream<HashCode> testHashes() {
    // Hash codes of zeros of various length
    Stream<HashCode> zeroHashCodes = IntStream.of(1, 2, 16, 32)
        .mapToObj(byte[]::new)
        .map(HashCode::fromBytes);

    // Non-zero 32-byte SHA-256 hash codes
    Stream<HashCode> sha256HashCodes = Stream.of(
        "",
        "a",
        "hello"
    ).map(s -> Hashing.sha256().hashString(s, StandardCharsets.UTF_8));
    return Streams.concat(zeroHashCodes, sha256HashCodes);
  }
}

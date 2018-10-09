package com.exonum.binding.common.serialization;

import static java.util.stream.Stream.concat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.test.Bytes;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

class StandardSerializersTest {

  /**
   * Performs a round trip tests: ObjectT -> Binary -> ObjectT.
   */
  static <ObjectT, SerializerT extends Serializer<ObjectT>> void roundTripTest(
      ObjectT expected, SerializerT serializer) {
    byte[] bytes = serializer.toBytes(expected);
    ObjectT actual = serializer.fromBytes(bytes);

    assertThat(actual, equalTo(expected));
  }

  /**
   * Performs check for invalid argument.
   */
  static void invalidBytesValueTest(byte[] invalidValue, Serializer serializer) {
    assertThrows(IllegalArgumentException.class, () -> serializer.fromBytes(invalidValue));
  }

  static IntStream intValues() {
    return IntStream.range(0, 32)
        .map(value -> 1 << value)
        .flatMap(value -> IntStream.of(-value, value - 1, value, value + 1));
  }

  static LongStream longValues() {
    return LongStream.range(0, 64)
        .map(value -> 1L << value)
        .flatMap(value -> LongStream.of(-value, value - 1, value, value + 1));
  }

  static Stream<byte[]> invalidVarints32() {
    return concat(malformedVarints(), Stream.of(
        // Exceeding the maximum length valid varints:
        Bytes.bytes(0x81, 0x82, 0x83, 0x84, 0x85, 0x06), // 6 bytes
        Bytes.bytes(0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x01) // 10 bytes
        )
    );
  }

  static Stream<byte[]> invalidVarints64() {
    return concat(malformedVarints(), Stream.of(
        // # MSB is set in the last byte, but there is the end of serialized value:
        Bytes.bytes(0x80, 0x81, 0x82, 0x83, 0x84, 0x85),
        Bytes.bytes(0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86),
        Bytes.bytes(0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87),
        Bytes.bytes(0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88),
        Bytes.bytes(0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89),
        // # Correct first bytes, but unexpected "tail":
        Bytes.bytes(0x80, 0x81, 0x82, 0x83, 0x04, 0x01), // A 6th byte is invalid
        // Exceeding the maximum length valid varints:
        Bytes.bytes(0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x90, 0x01) // 11 bytes
        )
    );
  }

  private static Stream<byte[]> malformedVarints() {
    return Stream.of(
        Bytes.bytes(),
        // # MSB is set in the last byte, but there is the end of serialized value:
        Bytes.bytes(0x80),
        Bytes.bytes(0x8F),
        Bytes.bytes(0xFF),
        Bytes.bytes(0x80, 0x81),
        Bytes.bytes(0x80, 0x81, 0x82),
        Bytes.bytes(0x80, 0x81, 0x82, 0x83),
        Bytes.bytes(0x80, 0x81, 0x82, 0x83, 0x84),

        // # Correct first bytes, but unexpected "tail":
        Bytes.bytes(0x01, 0x02), // A 1-byte tail
        Bytes.bytes(0x01, 0x82), // A 1-byte tail
        Bytes.bytes(0x01, 0x02, 0x03, 0x04, 0x85), // A 4-byte tail
        Bytes.bytes(0x80, 0x81, 0x02, 0x83, 0x84), // A 2-byte tail
        Bytes.bytes(0x80, 0x81, 0x82, 0x03, 0x84) // A 5th byte is invalid
    );
  }
}

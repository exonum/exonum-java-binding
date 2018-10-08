package com.exonum.binding.common.serialization;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

}

package com.exonum.binding.common.serialization;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class StandardSerializersTest {

  @Test
  void noop() {
    assertThat(StandardSerializers.noOp(), is(NoOpSerializer.INSTANCE));
  }

  @Test
  void bool() {
    assertThat(StandardSerializers.bool(), is(BoolSerializer.INSTANCE));
  }

  @Test
  void fixed32() {
    assertThat(StandardSerializers.fixed32(), is(Fixed32Serializer.INSTANCE));
  }

  @Test
  void fixed64() {
    assertThat(StandardSerializers.fixed64(), is(Fixed64Serializer.INSTANCE));
  }

  @Test
  void floats() {
    assertThat(StandardSerializers.floats(), is(FloatSerializer.INSTANCE));
  }

  @Test
  void doubles() {
    assertThat(StandardSerializers.doubles(), is(DoubleSerializer.INSTANCE));
  }

  @Test
  void string() {
    assertThat(StandardSerializers.string(), is(StringSerializer.INSTANCE));
  }

  @Test
  void hash() {
    assertThat(StandardSerializers.hash(), is(HashCodeSerializer.INSTANCE));
  }

  @Test
  void publicKey() {
    assertThat(StandardSerializers.publicKey(), is(PublicKeySerializer.INSTANCE));
  }

  @Test
  void privateKey() {
    assertThat(StandardSerializers.privateKey(), is(PrivateKeySerializer.INSTANCE));
  }

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
   *
   * @param invalidValue serialized value
   * @param serializer serializer
   */
  static void invalidBytesValueTest(byte[] invalidValue, Serializer serializer) {
    assertThrows(IllegalArgumentException.class, () -> serializer.fromBytes(invalidValue));
  }

}

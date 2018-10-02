/*
 * Copyright 2018 The Exonum Team
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

package com.exonum.binding.common.serialization;

import com.exonum.binding.common.crypto.PrivateKey;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.MessageLite;

/**
 * A collection of pre-defined serializers.
 */
public final class StandardSerializers {

  /**
   * Returns a serializer that does nothing.
   *
   * @return no-op serializer
   */
  public static Serializer<byte[]> noOp() {
    return NoOpSerializer.INSTANCE;
  }

  /**
   * Returns a serializer of booleans in little-endian byte order.
   *
   * @return boolean serializer
   */
  public static Serializer<Boolean> bool() {
    return BoolSerializer.INSTANCE;
  }

  /**
   * Returns a serializer of fixed32 (integer) in little-endian byte order.
   *
   * @return fixed32 serializer
   */
  public static Serializer<Integer> fixed32() {
    return Fixed32Serializer.INSTANCE;
  }

  /**
   * Returns a serializer of fixed64 (long) in little-endian byte order.
   *
   * @return fixed64 serializer
   */
  public static Serializer<Long> fixed64() {
    return Fixed64Serializer.INSTANCE;
  }

  /**
   * Returns a serializer of floats in little-endian byte order.
   *
   * @return float serializer
   */
  public static Serializer<Float> floats() {
    return FloatSerializer.INSTANCE;
  }

  /**
   * Returns a serializer of doubles in little-endian byte order.
   *
   * @return double serializer
   */
  public static Serializer<Double> doubles() {
    return DoubleSerializer.INSTANCE;
  }

  /**
   * Returns a serializer of strings in UTF-8.
   *
   * @return string serializer
   */
  public static Serializer<String> string() {
    return StringSerializer.INSTANCE;
  }

  /**
   * Returns a serializer of hash codes.
   *
   * @return hash code serializer
   */
  public static Serializer<HashCode> hash() {
    return HashCodeSerializer.INSTANCE;
  }

  /**
   * Returns a serializer of public keys.
   *
   * @return public key serializer
   */
  public static Serializer<PublicKey> publicKey() {
    return PublicKeySerializer.INSTANCE;
  }

  /**
   * Returns a serializer of private keys.
   *
   * @return private key serializer
   */
  public static Serializer<PrivateKey> privateKey() {
    return PrivateKeySerializer.INSTANCE;
  }

  /**
   * Returns a serializer for the given protocol buffer message type. The returned serializer
   * uses {@linkplain CodedOutputStream#useDeterministicSerialization() deterministic}
   * serialization mode.
   *
   * @param messageType the class of a protobuf message
   * @param <MessageT> the type of a message; must have a public static
   *    {@code #parseFrom(byte[])} method — as any auto-generated protobuf message does
   * @throws IllegalArgumentException if {@code MessageT} does not contain the static
   *    factory method {@code #parseFrom(byte[])}
   */
  public static <MessageT extends MessageLite> Serializer<MessageT> protobuf(
      Class<MessageT> messageType) {
    return new ProtobufReflectiveSerializer<>(messageType);
  }

  private StandardSerializers() {
  }
}

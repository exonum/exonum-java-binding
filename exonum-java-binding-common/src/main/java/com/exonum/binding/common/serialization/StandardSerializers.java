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

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.common.hash.HashCode;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.MessageLite;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * A collection of pre-defined serializers.
 */
// todo: test these guys with the code from exonum-serialization, when we migrate to JUnit5
// (ECR-642)
public final class StandardSerializers {

  /** Returns a serializer of longs in little-endian byte order. */
  public static Serializer<Long> longs() {
    return LongSerializer.INSTANCE;
  }

  /** Returns a serializer of strings in UTF-8. */
  public static Serializer<String> string() {
    return StringSerializer.INSTANCE;
  }

  /** Returns a serializer of hash codes. */
  public static Serializer<HashCode> hash() {
    return HashCodeSerializer.INSTANCE;
  }

  /**
   * Returns a serializer for the given protocol buffer message type. The returned serializer
   * uses {@linkplain CodedOutputStream#useDeterministicSerialization() deterministic}
   * serialization mode.
   *
   * @param messageType the class of a protobuf message
   * @param <MessageT> the type of a message; must have a static
   *     {@code #parseFrom(byte[])} method — as any auto-generated protobuf message does
   * @throws IllegalArgumentException if {@code MessageT} does not contain the static
   *     factory method {@code #parseFrom(byte[])}
   */
  public static <MessageT extends MessageLite> Serializer<MessageT> protobuf(
      Class<MessageT> messageType) {
    return new ProtobufSerializer<>(messageType);
  }

  enum LongSerializer implements Serializer<Long> {
    INSTANCE;

    @Override
    public byte[] toBytes(Long value) {
      ByteBuffer buf = ByteBuffer.allocate(Long.BYTES)
          .order(ByteOrder.LITTLE_ENDIAN);
      buf.putLong(value);
      return buf.array();
    }

    @Override
    public Long fromBytes(byte[] serializedValue) {
      checkArgument(serializedValue.length == Long.BYTES,
          "Expected an array of size 8, but was %s", serializedValue.length);

      return ByteBuffer.wrap(serializedValue)
          .order(ByteOrder.LITTLE_ENDIAN)
          .getLong();
    }
  }

  enum StringSerializer implements Serializer<String> {
    INSTANCE;

    @Override
    public byte[] toBytes(String value) {
      return value.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String fromBytes(byte[] serializedValue) {
      try {
        // Since the String(bytes, charset) constructor is specified so that
        // it "… always replaces malformed-input and unmappable-character sequences …",
        // it is not suitable for our use-case: we must reject malformed input.

        // Create a new decoder
        CharsetDecoder decoder = StandardCharsets.UTF_8
            .newDecoder()
            // Reject (= report as exception) malformed input.
            .onMalformedInput(CodingErrorAction.REPORT)
            // In case some valid UTF-8 characters are not encodable in UTF-16,
            // we replace them with the default replacement character.
            .onUnmappableCharacter(CodingErrorAction.REPLACE);

        // Decode the buffer in a character buffer
        CharBuffer strBuffer = decoder.decode(ByteBuffer.wrap(serializedValue));
        return new String(strBuffer.array(), strBuffer.arrayOffset(), strBuffer.remaining());
      } catch (CharacterCodingException e) {
        throw new IllegalArgumentException("Cannot decode the input", e);
      }
    }
  }

  enum HashCodeSerializer implements Serializer<HashCode> {
    INSTANCE;

    @Override
    public byte[] toBytes(HashCode value) {
      return value.asBytes();
    }

    @Override
    public HashCode fromBytes(byte[] serializedValue) {
      return HashCode.fromBytes(serializedValue);
    }
  }

  private StandardSerializers() {}
}

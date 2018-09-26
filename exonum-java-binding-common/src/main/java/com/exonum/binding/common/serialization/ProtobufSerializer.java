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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * A reflective serializer-adapter of protobuf messages.
 *
 * <p>Uses {@linkplain CodedOutputStream#useDeterministicSerialization() deterministic}
 * protocol buffer serialization mode.
 *
 * @param <MessageT> a type of a protobuf message. Usually, autogenerated with protoc
 */
class ProtobufSerializer<MessageT extends MessageLite> implements Serializer<MessageT> {

  /**
   * The handle to a static `MessageT#parseFrom(byte[]) -> MessageT`.
   */
  private final MethodHandle messageParseFrom;

  ProtobufSerializer(Class<MessageT> messageType) {
    MethodHandles.Lookup lookup = MethodHandles.publicLookup();
    try {
      messageParseFrom = lookup
          .findStatic(messageType, "parseFrom", MethodType.methodType(messageType, byte[].class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new IllegalArgumentException("Invalid message: cannot find public static parseFrom "
          + "method in " + messageType, e);
    }
  }

  @Override
  public byte[] toBytes(MessageT value) {
    // Essentially, the same as com.google.protobuf.AbstractMessageLite.toByteArray,
    // but uses a deterministic mode of CodedOutputStream.
    byte[] result = new byte[value.getSerializedSize()];

    CodedOutputStream output = CodedOutputStream.newInstance(result);
    output.useDeterministicSerialization();

    try {
      value.writeTo(output);
      output.checkNoSpaceLeft();
      return result;
    } catch (IOException e) {
      throw new AssertionError("Failed to serialize " + value
          + " to a byte array (should never happen)", e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public MessageT fromBytes(byte[] serializedValue) {
    checkNotNull(serializedValue);
    try {
      return (MessageT) messageParseFrom.invoke(serializedValue);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(e);
    } catch (Throwable throwable) {
      // MessageT#parseFrom is not supposed to throw anything but NPE
      // and InvalidProtocolBufferException
      throw new AssertionError("Unexpected exception in MessageT#parseFrom", throwable);
    }
  }
}

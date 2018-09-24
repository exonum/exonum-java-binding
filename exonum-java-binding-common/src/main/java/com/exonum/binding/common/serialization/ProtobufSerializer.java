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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class ProtobufSerializer<MessageT extends MessageLite> implements Serializer<MessageT> {

  private final MethodHandle messageParseFrom;

  ProtobufSerializer(Class<MessageT> messageType) {
    MethodHandles.Lookup lookup = MethodHandles.publicLookup();
    try {
      messageParseFrom = lookup
          .findStatic(messageType, "parseFrom", MethodType.methodType(messageType, byte[].class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new IllegalArgumentException("Invalid message: cannot find static parseFrom method",
          e);
    }
  }

  @Override
  public byte[] toBytes(MessageT value) {
    return value.toByteArray();
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
      throw new AssertionError("Unexpected exception during de-serialization", throwable);
    }
  }
}

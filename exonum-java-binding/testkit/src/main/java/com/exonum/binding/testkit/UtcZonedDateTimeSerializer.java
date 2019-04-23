/*
 * Copyright 2019 The Exonum Team
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
 *
 */

package com.exonum.binding.testkit;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.common.serialization.Serializer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

// TODO: duplicate of serializer in time-oracle - move to core? make it public in time-oracle?
/**
 * ZonedDateTime serializer. Only serializes values with UTC zones, throws an exception otherwise.
 */
enum UtcZonedDateTimeSerializer implements Serializer<ZonedDateTime> {
  INSTANCE;

  private static final int SERIALIZED_DATE_TIME_SIZE = Long.BYTES + Integer.BYTES;

  @Override
  public byte[] toBytes(ZonedDateTime value) {
    checkArgument(value.getZone() == ZoneOffset.UTC,
        "ZonedDateTime value should be in UTC, but was %s",
        value.getZone());
    long seconds = value.toEpochSecond();
    int nanos = value.getNano();
    ByteBuffer buffer = ByteBuffer.allocate(SERIALIZED_DATE_TIME_SIZE)
        .order(ByteOrder.LITTLE_ENDIAN);
    buffer.putLong(seconds);
    buffer.putInt(nanos);
    return buffer.array();
  }

  @Override
  public ZonedDateTime fromBytes(byte[] serializedValue) {
    checkArgument(serializedValue.length == SERIALIZED_DATE_TIME_SIZE,
        "Expected an array of size %s, but was %s", SERIALIZED_DATE_TIME_SIZE,
        serializedValue.length);

    ByteBuffer buffer = ByteBuffer.wrap(serializedValue)
        .order(ByteOrder.LITTLE_ENDIAN);
    return retrieveZdtFromBuffer(buffer);
  }

  private ZonedDateTime retrieveZdtFromBuffer(ByteBuffer buffer) {
    long seconds = buffer.getLong();
    int nanos = buffer.getInt();
    Instant instant = Instant.ofEpochSecond(seconds, nanos);
    return ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
  }
}

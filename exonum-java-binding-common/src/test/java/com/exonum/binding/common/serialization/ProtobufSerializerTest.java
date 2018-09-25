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

import static com.exonum.binding.common.serialization.StandardSerializersRoundtripTest.roundTripTest;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.serialization.TestProtos.Point;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ProtobufSerializerTest {

  private ProtobufSerializer<Point> serializer = new ProtobufSerializer<>(Point.class);

  @Test
  void toBytes() {
    Point p = createPoint(-1, 1);

    assertThat(serializer.toBytes(p), equalTo(p.toByteArray()));
  }

  @Test
  void toBytesNullMessage() {
    assertThrows(NullPointerException.class, () -> serializer.toBytes(null));
  }

  @Test
  void fromBytesNull() {
    assertThrows(NullPointerException.class, () -> serializer.fromBytes(null));
  }

  @Test
  void fromBytesInvalidInput() {
    byte[] invalidBuffer = new byte[32]; // Too big for a Point message

    assertThrows(IllegalArgumentException.class, () -> serializer.fromBytes(invalidBuffer));
  }

  @ParameterizedTest
  @MethodSource("testPoints")
  void roundtripTest(Point p) {
    roundTripTest(p, serializer);
  }

  private static Collection<Point> testPoints() {
    return ImmutableList.of(
        createPoint(0, 0),
        createPoint(1, 1),
        createPoint(-1, -1),
        createPoint(Integer.MIN_VALUE, Integer.MAX_VALUE)
    );
  }

  private static Point createPoint(int x, int y) {
    return Point.newBuilder()
        .setX(x)
        .setY(y)
        .build();
  }
}

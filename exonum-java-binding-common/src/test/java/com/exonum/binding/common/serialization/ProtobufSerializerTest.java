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
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.exonum.binding.common.serialization.TestProtos.Point;
import com.exonum.binding.common.serialization.TestProtos.Targets;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.MessageLite;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ProtobufSerializerTest {

  private ProtobufSerializer<Point> serializer = new ProtobufSerializer<>(Point.class);

  @Test
  void constructorRejectsInvalidMessages() {
    MessageLite m = mock(MessageLite.class); // Does not have a public static parseFrom.
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> new ProtobufSerializer<>(m.getClass()));

    assertThat(e.getMessage(),
        containsString("Invalid message: cannot find public static parseFrom"));

    assertThat(e.getCause(), instanceOf(NoSuchMethodException.class));
  }

  @Test
  void toBytes() {
    Point p = createPoint(-1, 1);

    assertThat(serializer.toBytes(p), equalTo(p.toByteArray()));
  }

  @Test
  void toBytesIsDeterministic() {
    Point p1 = createPoint(-1, -1);
    Point p2 = createPoint(1, 1);
    Point p3 = createPoint(Integer.MIN_VALUE, Integer.MAX_VALUE);

    Targets t1 = pointsAsTargetsInOrder(p1, p2, p3);
    Targets t2 = pointsAsTargetsInOrder(p3, p2, p1);

    ProtobufSerializer<Targets> serializer = new ProtobufSerializer<>(Targets.class);

    assertThat("ProtobufSerializer is not deterministic:",
        serializer.toBytes(t1), equalTo(serializer.toBytes(t2)));
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

  private static Targets pointsAsTargetsInOrder(Point... points) {
    // Use LinkedHashMap to guarantee that iteration order matches insertion order.
    Map<String, Point> orderedPoints = new LinkedHashMap<>(points.length);
    for (Point p : points) {
      orderedPoints.put(p.toString(), p);
    }

    return Targets.newBuilder()
        .putAllTargets(orderedPoints)
        .build();
  }
}

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

import com.exonum.binding.common.serialization.TestProtos.Point;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ProtobufSerializerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ProtobufSerializer<Point> serializer = new ProtobufSerializer<>(Point.class);

  @Test
  public void toBytes() {
    Point p = createPoint(-1, 1);

    assertThat(serializer.toBytes(p), equalTo(p.toByteArray()));
  }

  @Test
  public void toBytesNullMessage() {
    expectedException.expect(NullPointerException.class);

    serializer.toBytes(null);
  }

  @Test
  public void fromBytesNull() {
    expectedException.expect(NullPointerException.class);

    serializer.fromBytes(null);
  }

  @Test
  public void fromBytesInvalidInput() {
    byte[] invalidBuffer = new byte[32]; // Too big for a Point message

    expectedException.expect(IllegalArgumentException.class);

    serializer.fromBytes(invalidBuffer);
  }

  @Test
  public void roundtripTest() {
    List<Point> valuesToTest = ImmutableList.of(
        createPoint(0, 0),
        createPoint(1, 1),
        createPoint(-1, -1)
    );

    valuesToTest.forEach(v -> roundTripTest(v, StandardSerializers.protobuf(Point.class)));
  }

  private static Point createPoint(int x, int y) {
    return Point.newBuilder()
        .setX(x)
        .setY(y)
        .build();
  }
}

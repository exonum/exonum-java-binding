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

package com.exonum.binding.common.serialization.json;

import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.google.common.collect.ImmutableList;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ZonedDateTimeGsonSerializerTest {

  @ParameterizedTest
  @MethodSource("source")
  void roundTripTest(ZonedDateTime zonedDateTime) {
    String json = json().toJson(zonedDateTime);
    ZonedDateTime restoredMsg = json().fromJson(json, ZonedDateTime.class);

    assertThat(restoredMsg, is(zonedDateTime));
  }

  private static List<ZonedDateTime> source() {
    return ImmutableList.of(
        ZonedDateTime.now(ZoneOffset.UTC),
        ZonedDateTime.of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC),
        ZonedDateTime.of(1969, 7, 21, 6, 8, 21, 1000, ZoneId.of("Europe/Kiev"))
    );
  }

}

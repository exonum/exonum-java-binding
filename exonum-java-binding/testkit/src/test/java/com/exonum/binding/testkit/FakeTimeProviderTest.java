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
 */

package com.exonum.binding.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import org.junit.jupiter.api.Test;

class FakeTimeProviderTest {

  private static final ZonedDateTime TIME =
      ZonedDateTime.of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
  private static final ZonedDateTime NOT_UTC_TIME =
      ZonedDateTime.of(2000, 1, 1, 1, 1, 1, 1, ZoneId.of("Europe/Amsterdam"));

  @Test
  void createFakeTimeProvider() {
    FakeTimeProvider timeProvider = FakeTimeProvider.create(TIME);
    assertThat(timeProvider.getTime()).isEqualTo(TIME);
  }

  @Test
  void createFakeTimeProviderRejectsInvalidTimeZone() {
    assertThrows(IllegalArgumentException.class, () -> FakeTimeProvider.create(NOT_UTC_TIME));
  }

  @Test
  void setTime() {
    FakeTimeProvider timeProvider = FakeTimeProvider.create(TIME);
    ZonedDateTime newTime = ZonedDateTime.of(2010, 10, 10, 10, 10, 10, 10, ZoneOffset.UTC);
    timeProvider.setTime(newTime);
    assertThat(timeProvider.getTime()).isEqualTo(newTime);
  }

  @Test
  void setTimeRejectsInvalidTimeZone() {
    FakeTimeProvider timeProvider = FakeTimeProvider.create(TIME);
    assertThrows(IllegalArgumentException.class, () -> timeProvider.setTime(NOT_UTC_TIME));
  }

  @Test
  void addTime() {
    FakeTimeProvider timeProvider = FakeTimeProvider.create(TIME);
    TemporalAmount toAdd = Duration.ofDays(9);
    ZonedDateTime expectedTime = ZonedDateTime.of(2000, 1, 10, 1, 1, 1, 1, ZoneOffset.UTC);
    timeProvider.addTime(toAdd);
    assertThat(timeProvider.getTime()).isEqualTo(expectedTime);
  }
}

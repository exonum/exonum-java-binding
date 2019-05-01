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

import static com.google.common.base.Preconditions.checkArgument;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;

/**
 * Fake time provider for service testing. Allows to manually manipulate time that is returned
 * by TestKit time service. If you need to set results of different consecutive calls on
 * {@link #getTime()}, consider using a mock of TimeProvider instead.
 */
public class FakeTimeProvider implements TimeProvider {

  // As native code can access this field at any time, it is made volatile so that any changes to
  // it are visible immediately
  private volatile ZonedDateTime time;

  private FakeTimeProvider(ZonedDateTime time) {
    this.time = time;
  }

  /**
   * Creates a fake time provider with given time. Note that time should be in UTC time zone.
   *
   * @throws IllegalArgumentException if value has time zone other than UTC
   */
  public static FakeTimeProvider create(ZonedDateTime initialTime) {
    checkTimeZone(initialTime);
    return new FakeTimeProvider(initialTime);
  }

  /**
   * Sets new time for this time provider. Note that time should be in UTC time zone.
   *
   * @throws IllegalArgumentException if value has time zone other than UTC
   */
  public void setTime(ZonedDateTime time) {
    checkTimeZone(time);
    this.time = time;
  }

  private static void checkTimeZone(ZonedDateTime value) {
    checkArgument(value.getZone() == ZoneOffset.UTC,
        "ZonedDateTime value should be in UTC, but was %s",
        value.getZone());
  }

  /**
   * Increases stored time by given amount.
   */
  public void addTime(TemporalAmount toAdd) {
    time = time.plus(toAdd);
  }

  @Override
  public ZonedDateTime getTime() {
    return time;
  }
}

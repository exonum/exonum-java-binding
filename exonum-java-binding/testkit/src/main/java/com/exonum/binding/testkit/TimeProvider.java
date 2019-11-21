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

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Time provider for service testing. Used as a time source by TestKit time service.
 */
public interface TimeProvider {

  /**
   * Returns the current time of this time provider in UTC time zone.
   */
  ZonedDateTime getTime();

  /**
   * Returns a provider that uses the {@linkplain Clock#system system time}.
   */
  static TimeProvider systemTime() {
    return () -> ZonedDateTime.now(ZoneOffset.UTC);
  }
}

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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class SystemTimeProviderTest {

  @Test
  void systemTimeProvidesCurrentTimeInUtc() {
    TimeProvider systemTime = TimeProvider.systemTime();

    ZonedDateTime before = ZonedDateTime.now(ZoneOffset.UTC);
    ZonedDateTime provided = systemTime.getTime();
    ZonedDateTime after = ZonedDateTime.now(ZoneOffset.UTC);

    // Check that the provided time uses the UTC time zone and the current time
    assertThat(provided).isBetween(before, after);
  }
}

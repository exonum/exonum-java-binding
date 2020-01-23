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

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.time.UtcZonedDateTimeSerializer;
import java.time.ZonedDateTime;

/** An adapter of a {@link TimeProvider} for native code. */
final class TimeProviderAdapter {
  private static final Serializer<ZonedDateTime> ZDT_SERIALIZER =
      UtcZonedDateTimeSerializer.INSTANCE;

  private final TimeProvider timeProvider;

  TimeProviderAdapter(TimeProvider timeProvider) {
    this.timeProvider = checkNotNull(timeProvider, "TimeProvider must not be null");
  }

  @SuppressWarnings("unused") // Used in native code
  byte[] getTime() {
    return ZDT_SERIALIZER.toBytes(timeProvider.getTime());
  }
}

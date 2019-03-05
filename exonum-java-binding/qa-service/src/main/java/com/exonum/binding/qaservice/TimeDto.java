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

package com.exonum.binding.qaservice;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import java.time.ZonedDateTime;

/**
 * DTO class for JSON view.
 */
final class TimeDto {
  private final ZonedDateTime time;

  TimeDto(ZonedDateTime time) {
    this.time = time;
  }

  ZonedDateTime getTime() {
    return time;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TimeDto timeDTO = (TimeDto) o;
    return Objects.equal(time, timeDTO.time);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(time);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("time", time)
        .toString();
  }
}

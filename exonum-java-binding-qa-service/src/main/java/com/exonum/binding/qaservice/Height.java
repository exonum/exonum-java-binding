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
 *
 */

package com.exonum.binding.qaservice;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * DTO class for JSON view.
 */
final class Height {
  private final long height;

  Height(long height) {
    this.height = height;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Height that = (Height) o;
    return height == that.height;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(height);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("height", height)
        .toString();
  }
}

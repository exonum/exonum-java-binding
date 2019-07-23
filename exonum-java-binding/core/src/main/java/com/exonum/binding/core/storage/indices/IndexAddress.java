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

package com.exonum.binding.core.storage.indices;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An Exonum index address: a pair of the name and an optional family id, which identifies
 * an Exonum index.
 */
public final class IndexAddress {

  private final String name;
  @Nullable private final byte[] familyId;

  public IndexAddress(String name) {
    this(name, null);
  }

  public IndexAddress(String name, @Nullable byte[] familyId) {
    this.name = checkNotNull(name);
    this.familyId = familyId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof IndexAddress)) {
      return false;
    }
    IndexAddress that = (IndexAddress) o;
    return name.equals(that.name) &&
        Arrays.equals(familyId, that.familyId);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(name);
    result = 31 * result + Arrays.hashCode(familyId);
    return result;
  }
}

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

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.common.hash.HashCode;
import java.util.Optional;

class MapTestEntry {

  private final HashCode key;

  private final String value;

  private MapTestEntry(HashCode key, String value) {
    this.key = key;
    this.value = value;
  }

  static MapTestEntry presentEntry(HashCode key, String value) {
    checkArgument(value != null, "Value shouldn't be null in present entry");
    return new MapTestEntry(key, value);
  }

  static MapTestEntry absentEntry(HashCode key) {
    return new MapTestEntry(key, null);
  }

  public HashCode getKey() {
    return key;
  }

  public Optional<String> getValue() {
    return Optional.ofNullable(value);
  }
}

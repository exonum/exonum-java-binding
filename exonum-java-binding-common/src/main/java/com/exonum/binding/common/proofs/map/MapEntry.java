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
 */

package com.exonum.binding.common.proofs.map;

import java.util.Arrays;

/**
 * A map entry: a key-value pair. This entry does not permit null keys and values.
 */
public class MapEntry {
  private final byte[] key;

  private final byte[] value;

  /**
   * Creates a new entry in a flat map proof corresponding to a leaf node.
   * @param key a node key
   * @param value a value mapped to the key
   */
  public MapEntry(byte[] key, byte[] value) {
    this.key = key;
    this.value = value;
  }

  /** Returns the key in this entry. */
  public byte[] getKey() {
    return key;
  }

  /** Returns the value in this entry. */
  public byte[] getValue() {
    return value;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MapEntry that = (MapEntry) o;
    return Arrays.equals(key, that.key) && Arrays.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(key) ^ Arrays.hashCode(value);
  }
}

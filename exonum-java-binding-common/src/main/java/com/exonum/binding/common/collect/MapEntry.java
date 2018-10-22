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

package com.exonum.binding.common.collect;

import com.exonum.binding.common.proofs.map.ByteStringMapEntry;
import com.google.protobuf.ByteString;

/**
 * MapEntry common interface which provides several factory methods for {@link ByteStringMapEntry}
 * and {@link GenericMapEntry} creation.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public interface MapEntry<K, V> {

  /**
   * Creates {@link ByteStringMapEntry} from provided key and value in byte array format.
   */
  static ByteStringMapEntry valueOf(byte[] key, byte[] value) {
    return new ByteStringMapEntry(key, value);
  }

  /**
   * Creates {@link ByteStringMapEntry} from provided {@link ByteString} key and value.
   */
  static ByteStringMapEntry valueOf(ByteString key, ByteString value) {
    return new ByteStringMapEntry(key, value);
  }

  /**
   * Creates {@link GenericMapEntry} from provided key and value.
   */
  static <K, V> GenericMapEntry<K, V> valueOf(K key, V value) {
    return new com.exonum.binding.common.collect.AutoValue_GenericMapEntry<>(key, value);
  }

  /** Returns the key in this entry. */
  K getKey();

  /** Returns the value in this entry. */
  V getValue();
}

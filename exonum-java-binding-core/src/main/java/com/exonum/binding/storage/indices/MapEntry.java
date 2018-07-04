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

package com.exonum.binding.storage.indices;

import com.exonum.binding.storage.serialization.Serializer;
import com.google.auto.value.AutoValue;

/**
 * A map entry: a key-value pair. This entry does not permit null keys and values.
 *
 * <p>A map entry contains <em>a copy</em> of the data in the corresponding map index.
 * It does not reflect the changes made to the map since this entry had been created.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@AutoValue
public abstract class MapEntry<K, V> {

  /** Returns the key in this entry. */
  public abstract K getKey();

  /** Returns the value in this entry. */
  public abstract V getValue();

  static <K, V> MapEntry<K, V> fromInternal(MapEntryInternal e,
                                            Serializer<K> keySerializer,
                                            Serializer<V> valueSerializer) {
    K key = keySerializer.fromBytes(e.key);
    V value = valueSerializer.fromBytes(e.value);
    return from(key, value);
  }

  static <K, V> MapEntry<K, V> from(K key, V value) {
    return new AutoValue_MapEntry<>(key, value);
  }
}

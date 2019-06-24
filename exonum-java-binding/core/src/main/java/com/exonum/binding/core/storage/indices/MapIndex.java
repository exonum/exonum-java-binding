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

import com.exonum.binding.common.collect.MapEntry;
import com.exonum.binding.core.storage.database.Fork;
import java.util.Iterator;
import java.util.Map;

/**
 * A MapIndex is an index that maps keys to values. A map cannot contain duplicate keys;
 * each key corresponds to at most one value.
 *
 * <p>The "destructive" methods of the map, i.e., the one that change the map contents,
 * are specified to throw {@link UnsupportedOperationException} if
 * the map has been created with a read-only database view.
 *
 * <p>This interface prohibits null keys and values.
 *
 * @param <K> the type of keys in this map
 * @param <V> the type of values in this map
 */
public interface MapIndex<K, V> extends StorageIndex {

  /**
   * Returns true if this map contains a mapping for the specified key.
   *
   * @throws IllegalStateException if this map is not valid
   */
  boolean containsKey(K key);

  /**
   * Puts a new key-value pair into the map. If this map already contains
   * a mapping for the specified key, overwrites the old value with the specified value.
   *
   * @param key a storage key
   * @param value a storage value to associate with the key
   * @throws IllegalStateException if this map is not valid
   * @throws IllegalArgumentException if some property of the key or the value prevents it
   *                                  from being stored in this map
   * @throws UnsupportedOperationException if this map is read-only
   */
  void put(K key, V value);

  /**
   * Puts all key-value pairs from the given map into this map. Equivalent to a sequence
   * of individual {@link #put} operations.
   *
   * @param sourceMap a map to put into this one
   * @throws NullPointerException if the passed map is null or contains a null key or values
   * @throws IllegalStateException if this map is not valid
   * @throws IllegalArgumentException if some property of the key or the value prevents it
   *                                  from being stored in this map
   * @throws UnsupportedOperationException if this map is read-only
   */
  default void putAll(Map<? extends K, ? extends V> sourceMap) {
    for (Map.Entry<? extends K, ? extends V> entry : sourceMap.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Returns the value associated with the specified key,
   * or {@code null} if there is no mapping for the key.
   *
   * @param key a storage key
   * @return the value mapped to the specified key,
   *         or {@code null} if this map contains no mapping for the key.
   * @throws IllegalStateException if this map is not valid
   */
  V get(K key);

  /**
   * Removes the value mapped to the specified key from the map.
   * If there is no such mapping, has no effect.
   *
   * @param key a storage key
   * @throws IllegalStateException if this map is not valid
   * @throws UnsupportedOperationException if this map is read-only
   */
  void remove(K key);
  
  /**
   * Returns an iterator over the map keys in lexicographical order.
   *
   * <p>Any destructive operation on the same {@link Fork} this map uses
   * (but not necessarily on <em>this map</em>) will invalidate the iterator.
   *
   * @throws IllegalStateException if this map is not valid
   */
  Iterator<K> keys();

  /**
   * Returns an iterator over the map values in lexicographical order of <em>keys</em>.
   *
   * <p>Any destructive operation on the same {@link Fork} this map uses
   * (but not necessarily on <em>this map</em>) will invalidate the iterator.
   *
   * @throws IllegalStateException if this map is not valid
   */
  Iterator<V> values();

  /**
   * Returns an iterator over the map entries.
   * The entries are ordered by keys in lexicographical order.
   *
   * <p>Any destructive operation on the same {@link Fork} this map uses
   * (but not necessarily on <em>this map</em>) will invalidate the iterator.
   *
   * @throws IllegalStateException if this map is not valid
   */
  Iterator<MapEntry<K, V>> entries();

  /**
   * Removes all of the key-value pairs from the map.
   * The map will be empty after this method returns.
   *
   * @throws IllegalStateException if this map is not valid
   * @throws UnsupportedOperationException if this map is read-only
   */
  void clear();

  /**
   * Returns true if this map has no entries.
   *
   * <p>Note: there is no {@code size()} method because
   * implementations of MapIndex do not currently track
   * the number of entries.
   */
  default boolean isEmpty() {
    return !keys().hasNext();
  }
}

/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.storage.indices;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class MapEntries {

  static <K, V> List<K> extractKeys(Collection<? extends MapEntry<K, V>> entries) {
    return entries.stream()
        .map(MapEntry::getKey)
        .collect(Collectors.toList());
  }

  static <K, V> List<V> extractValues(Collection<? extends MapEntry<K, V>> entries) {
    return entries.stream()
        .map(MapEntry::getValue)
        .collect(Collectors.toList());
  }

  static <K, V> Map<K, V> extractEntries(MapIndex<K, V> map) {
    Map<K, V> result = new LinkedHashMap<>();
    map.entries().forEachRemaining(
        e -> result.put(e.getKey(), e.getValue())
    );
    return result;
  }

  static <K, V> void putAll(MapIndex<K, V> map,
                            Collection<? extends MapEntry<? extends K, ? extends V>> entries) {
    for (MapEntry<? extends K, ? extends V> e : entries) {
      map.put(e.getKey(), e.getValue());
    }
  }

  private MapEntries() {}
}

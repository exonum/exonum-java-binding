package com.exonum.binding.storage.indices;

import java.util.Collection;
import java.util.List;
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

  static <K, V> void putAll(MapIndex<K, V> map,
                            Collection<? extends MapEntry<? extends K, ? extends V>> entries) {
    for (MapEntry<? extends K, ? extends V> e : entries) {
      map.put(e.getKey(), e.getValue());
    }
  }

  private MapEntries() {}
}

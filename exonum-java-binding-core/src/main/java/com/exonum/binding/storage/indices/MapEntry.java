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

  public abstract K getKey();

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

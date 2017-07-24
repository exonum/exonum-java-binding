package com.exonum.binding.proxy;

import static com.exonum.binding.proxy.StoragePreconditions.checkStorageKey;
import static com.exonum.binding.proxy.StoragePreconditions.checkStorageValue;

/**
 * A map entry: a key-value pair. This entry does not permit null keys and values.
 *
 * <p>A map entry contains <em>a copy</em> of the data in the corresponding map index.
 * It does not reflect the changes made to the map since this entry had been created.
 */
public class MapEntry {
  private final byte[] key;
  private final byte[] value;

  @SuppressWarnings("unused")  // native API
  MapEntry(byte[] key, byte[] value) {
    this.key = checkStorageKey(key);
    this.value = checkStorageValue(value);
  }

  public byte[] getKey() {
    return key;
  }

  public byte[] getValue() {
    return value;
  }
}

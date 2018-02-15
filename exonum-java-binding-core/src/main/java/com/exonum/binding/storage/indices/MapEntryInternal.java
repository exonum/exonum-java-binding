package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.StoragePreconditions.checkStorageKey;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkStorageValue;

final class MapEntryInternal {
  final byte[] key;
  final byte[] value;

  @SuppressWarnings("unused")  // native API
  MapEntryInternal(byte[] key, byte[] value) {
    this.key = checkStorageKey(key);
    this.value = checkStorageValue(value);
  }
}

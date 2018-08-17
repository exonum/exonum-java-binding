package com.exonum.binding.storage.proofs.map.flat;

import java.util.Arrays;

/**
 * A map entry: a key-value pair. This entry does not permit null keys and values.
 */
public class CheckedMapProofEntry {
  private final byte[] key;

  private final byte[] value;

  CheckedMapProofEntry(byte[] key, byte[] value) {
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
    CheckedMapProofEntry that = (CheckedMapProofEntry) o;
    return Arrays.equals(key, that.key) && Arrays.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(key) ^ Arrays.hashCode(value);
  }
}

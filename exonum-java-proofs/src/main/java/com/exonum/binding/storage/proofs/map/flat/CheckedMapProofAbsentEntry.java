package com.exonum.binding.storage.proofs.map.flat;

import java.util.Arrays;

/**
 * An entry, representing key that was requested, but not found in a map.
 */
public class CheckedMapProofAbsentEntry {
  private final byte[] key;

  CheckedMapProofAbsentEntry(byte[] key) {
    this.key = key;
  }

  /** Returns the key in this entry. */
  public byte[] getKey() {
    return key;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CheckedMapProofAbsentEntry that = (CheckedMapProofAbsentEntry) o;
    return Arrays.equals(key, that.key);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(key);
  }
}

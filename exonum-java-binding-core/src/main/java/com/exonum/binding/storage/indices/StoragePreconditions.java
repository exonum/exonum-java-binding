package com.exonum.binding.storage.indices;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

final class StoragePreconditions {

  static final int PROOF_MAP_KEY_SIZE = 32;

  private static final int MIN_INDEX_PREFIX_LENGTH = 1;

  /**
   * Checks that an index prefix is valid.
   *
   * @param prefix an index prefix.
   * @return an unmodified prefix if it's valid.
   * @throws NullPointerException if the prefix is null.
   * @throws IllegalArgumentException if the prefix has zero length.
   */
  static byte[] checkIndexPrefix(byte[] prefix) {
    checkArgument(MIN_INDEX_PREFIX_LENGTH <= prefix.length, "prefix has zero size");
    return prefix;
  }

  /**
   * Checks that a key is valid.
   *
   * @param key a storage key.
   * @return an unmodified key if it's valid.
   * @throws NullPointerException if key is null.
   */
  static byte[] checkStorageKey(byte[] key) {
    return checkNotNull(key, "Storage key is null");
  }

  /**
   * Checks that a proof map key is valid: not null and 32-byte long.
   *
   * @param key a proof map key
   * @return an unmodified key if it's valid
   * @throws NullPointerException if key is null
   * @throws IllegalArgumentException if the size of the key is not 32 bytes
   */
  static byte[] checkProofKey(byte[] key) {
    checkNotNull(key, "Proof map key is null");
    checkArgument(key.length == PROOF_MAP_KEY_SIZE,
        "Proof map key has invalid size: %s", key.length);
    return key;
  }

  /**
   * Checks that the specified collection contains valid storage values.
   *
   * @param values a storage value
   * @return an unmodified collection if all elements are valid
   * @throws NullPointerException if the collection is null or it contains null elements
   */
  static Collection<byte[]> checkStorageValues(Collection<byte[]> values) {
    values.forEach(StoragePreconditions::checkStorageValue);
    return values;
  }

  /**
   * Checks that a value is valid.
   *
   * @param value a storage value.
   * @return an unmodified value if it's valid.
   * @throws NullPointerException if value is null.
   */
  static byte[] checkStorageValue(byte[] value) {
    return checkNotNull(value, "Storage value is null");
  }

  /**
   * Checks that element index is valid, i.e., in range [0, size).
   *
   * @return a valid index
   * @throws IndexOutOfBoundsException if index is not in range [0, size).
   */
  static long checkElementIndex(long index, long size) {
    if (index < 0L || size <= index) {
      throw new IndexOutOfBoundsException("Index must be in range [0, " + size + "),"
          + " but: " + index);
    }
    return index;
  }

  private StoragePreconditions() {}
}

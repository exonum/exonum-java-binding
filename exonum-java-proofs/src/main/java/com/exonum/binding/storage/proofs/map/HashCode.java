package com.exonum.binding.storage.proofs.map;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.hash.Hashes;

/**
 * A SHA-256 hash code.
 */
public class HashCode {

  public static final int SIZE = Hashes.HASH_SIZE_BYTES;

  private final byte[] hash;

  public HashCode(byte[] hash) {
    checkArgument(hash.length == SIZE, "Invalid length: %s", hash.length);
    this.hash = hash;
  }

  public byte[] getHash() {
    return hash;
  }
}

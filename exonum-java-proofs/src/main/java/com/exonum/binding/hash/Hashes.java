package com.exonum.binding.hash;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class Hashes {

  /**
   * Size of a hash code in the default Exonum algorithm.
   */
  public static final int HASH_SIZE_BYTES = 32;

  /**
   * Size of a hash code in the default Exonum algorithm, in bits.
   */
  public static final int HASH_SIZE_BITS = HASH_SIZE_BYTES * Byte.SIZE;

  private static final int CHARACTERS_PER_BYTE = 2;

  /**
   * Returns the default Exonum hash function: SHA-256.
   *
   * @see HashFunction#newHasher()
   */
  public static HashFunction defaultHashFunction() {
    return Hashing.sha256();
  }

  /**
   * Returns the hash as a hexadecimal String, or «null» if it's null.
   */
  public static String toHexString(byte[] hash) {
    if (hash == null) {
      return "null";
    }
    StringBuilder str = new StringBuilder(CHARACTERS_PER_BYTE * hash.length);
    for (byte b : hash) {
      str.append(asHexChar((byte) ((b & 0xFF) >>> 4)))
          .append(asHexChar((byte) (b & 0xF)));
    }
    return str.toString();
  }

  private static char asHexChar(byte b) {
    if (0 <= b && b <= 0xF) {
      if (b < 0xA) {
        return (char) ('0' + b);
      } else {
        return (char) ('a' + (b - 10));
      }
    } else {
      throw new IllegalArgumentException("Not in range [0, 15]: b=" + b);
    }
  }
}

package com.exonum.binding.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hashes {

  public static final int HASH_SIZE_BYTES = 32;

  private static final String EXONUM_DEFAULT_HASHING_ALGORITHM = "SHA-256";

  private static final int CHARACTERS_PER_BYTE = 2;

  /**
   * Combines the specified byte arrays together and returns SHA-256 hash of the resulting sequence.
   * For example, {@code getHashOf(new byte[]{1}, new byte[]{2}, new byte[]{3})} produces
   * the same result as {@code getHashOf(new byte[]{1, 2, 3})}.
   *
   * @param items zero or more byte arrays to hash
   * @return SHA-256 hash of the combined byte array
   */
  public static byte[] getHashOf(byte[]... items) {
    try {
      MessageDigest sha256 = MessageDigest.getInstance(EXONUM_DEFAULT_HASHING_ALGORITHM);
      for (byte[] i : items) {
        sha256.update(i);
      }
      return sha256.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new Error(e);
    }
  }

  /**
   * Returns the hash as a hexadecimal String.
   */
  public static String toString(byte[] hash) {
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

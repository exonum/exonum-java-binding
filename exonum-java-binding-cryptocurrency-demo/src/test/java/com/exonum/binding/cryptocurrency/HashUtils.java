package com.exonum.binding.cryptocurrency;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;

/**
 * A utility class with some hash utils on top of {@link Hashing}.
 */
public final class HashUtils {

  /**
   * Hashes the input characters encoded in UTF-8 using the default Exonum hashing algorithm.
   *
   * @param input input characters to encode and hash
   * @return a SHA-256 hash of the input
   */
  public static HashCode hashUtf8String(CharSequence input) {
    return Hashing.defaultHashFunction().hashString(input, UTF_8);
  }

  private HashUtils() {}
}

package com.exonum.binding.storage.proofs.map;

import static com.exonum.binding.test.Bytes.createPrefixed;

import java.util.BitSet;

class DbKeyTestUtils {

  /**
   * Returns a new branch db key with the given prefix. The number of significant bits
   * is equal to the number of bits in the string (excluding whitespaces and delimiters).
   *
   * @param prefix a key prefix — from the least significant bit to the most significant,
   *               i.e., "00 01" is 8, "10 00" is 1.
   *               May contain spaces, underscores or bars (e.g., "00 01|01 11" and "11_10"
   *               are valid strings).
   */
  public static DbKey branchKeyFromPrefix(String prefix) {
    prefix = filterBitPrefix(prefix);
    byte[] key = keyFromString(prefix);
    return branchDbKey(key, prefix.length());
  }

  /**
   * Returns a new leaf db key with the given prefix.
   *
   * @param prefix a key prefix — from the least significant bit to the most significant,
   *               i.e., "00 01" is 8, "10 00" is 1.
   *               May contain spaces, underscores or bars (e.g., "00 01|01 11" and "11_10"
   *               are valid strings).
   */
  static DbKey leafKeyFromPrefix(String prefix) {
    prefix = filterBitPrefix(prefix);
    byte[] key = keyFromString(prefix);
    return leafDbKey(key);
  }

  /** Replaces spaces that may be used to separate groups of binary digits. */
  private static String filterBitPrefix(String prefix) {
    String filtered = prefix.replaceAll("[ _|]", "");
    // Check that the string is correct
    assert filtered.matches("[01]*");
    assert filtered.length() <= DbKey.KEY_SIZE_BITS;
    return filtered;
  }

  /** Creates a 32-byte key from the bit prefix. */
  private static byte[] keyFromString(String prefix) {
    BitSet keyPrefixBits = new BitSet(prefix.length());
    for (int i = 0; i < prefix.length(); i++) {
      char bit = prefix.charAt(i);
      if (bit == '1') {
        keyPrefixBits.set(i);
      }
    }
    return createPrefixed(keyPrefixBits.toByteArray(), DbKey.KEY_SIZE);
  }

  static DbKey leafDbKey(byte[] key) {
    return DbKey.newLeafKey(key);
  }

  static DbKey branchDbKey(byte[] key, int numSignificantBits) {
    return DbKey.newBranchKey(key, numSignificantBits);
  }
}

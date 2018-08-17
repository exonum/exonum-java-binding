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
    // Replace spaces that may be used to separate groups of binary digits
    prefix = prefix.replaceAll("[ _|]", "");
    // Check that the string is correct
    assert prefix.matches("[01]*");
    assert prefix.length() <= DbKey.KEY_SIZE_BITS;

    BitSet keyPrefixBits = new BitSet(prefix.length());
    for (int i = 0; i < prefix.length(); i++) {
      char bit = prefix.charAt(i);
      if (bit == '1') {
        keyPrefixBits.set(i);
      }
    }

    byte[] fullKeySlice = createPrefixed(keyPrefixBits.toByteArray(), DbKey.KEY_SIZE);
    return branchDbKey(fullKeySlice, prefix.length());
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
    // Replace spaces that may be used to separate groups of binary digits
    prefix = prefix.replaceAll("[ _|]", "");
    // Check that the string is correct
    assert prefix.matches("[01]*");
    assert prefix.length() <= DbKey.KEY_SIZE_BITS;

    BitSet keyPrefixBits = new BitSet(prefix.length());
    for (int i = 0; i < prefix.length(); i++) {
      char bit = prefix.charAt(i);
      if (bit == '1') {
        keyPrefixBits.set(i);
      }
    }

    byte[] fullKeySlice = createPrefixed(keyPrefixBits.toByteArray(), DbKey.KEY_SIZE);
    return leafDbKey(fullKeySlice);
  }

  static DbKey leafDbKey(byte[] key) {
    return DbKey.newLeafKey(key);
  }

  static DbKey branchDbKey(byte[] key, int numSignificantBits) {
    return DbKey.newBranchKey(key, numSignificantBits);
  }
}

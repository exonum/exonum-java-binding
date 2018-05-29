package com.exonum.binding.crypto;

/**
 * A collection of public-key signature system crypto functions.
 */
public final class CryptoFunctions {

  private CryptoFunctions() {}

  /**
   * Returns a ED25519 public-key signature system crypto function.
   */
  public static CryptoFunction ed25519() {
    return Ed25519CryptoFunction.INSTANCE;
  }
}

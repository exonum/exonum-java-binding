package com.exonum.binding.crypto;

public class CryptoFunctions {

  /**
   * Returns a ED25519 public-key signature system crypto function.
   */
  public static CryptoFunction ed25519() {
    return new Ed25519CryptoFunction();
  }
}

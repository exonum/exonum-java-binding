package com.exonum.binding.crypto;

public interface CryptoFunction {

  /**
   * Generates a private key and a corresponding public key using a {@code seed} byte array.
   */
  KeyPair generateKeyPair(byte[] seed);

  /**
   * Generates a private key and a corresponding public key using a random seed.
   */
  KeyPair generateKeyPair();

  /**
   * Given a {@code privateKey}, computes and returns a signature for the previously supplied
   * {@code message}.
   */
  byte[] signMessage(byte[] message, PrivateKey privateKey);

  /**
   * Given a {@code publicKey}, verifies that {@code signature} is a valid signature for the
   * supplied {@code message}.
   */
  boolean verify(byte[] message, byte[] signature, PublicKey publicKey);
}

package com.exonum.binding.crypto;

/**
 * A crypto function that provides several signature system crypto methods.
 */
public interface CryptoFunction {

  /**
   * Generates a private key and a corresponding public key using a {@code seed} byte array.
   * @throws IllegalArgumentException if the specified seed is not valid
   */
  KeyPair generateKeyPair(byte[] seed);

  /**
   * Generates a private key and a corresponding public key using a random seed.
   */
  KeyPair generateKeyPair();

  /**
   * Given a {@code privateKey}, computes and returns a signature for the supplied {@code message}.
   * @return signature as a byte array
   */
  byte[] signMessage(byte[] message, PrivateKey privateKey);

  /**
   * Given a {@code publicKey}, verifies that {@code signature} is a valid signature for the
   * supplied {@code message}.
   * @return true if signature is valid, false otherwise
   */
  boolean verify(byte[] message, byte[] signature, PublicKey publicKey);
}

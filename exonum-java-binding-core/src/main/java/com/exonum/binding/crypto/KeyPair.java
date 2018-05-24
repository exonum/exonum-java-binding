package com.exonum.binding.crypto;

/**
 * A key pair class that stores public and private keys.
 */
public class KeyPair {

  private final PublicKey publicKey;
  private final PrivateKey privateKey;

  private KeyPair(byte[] privateKey, byte[] publicKey) {
    this.privateKey = PrivateKey.fromBytesNoCopy(privateKey);
    this.publicKey = PublicKey.fromBytesNoCopy(publicKey);
  }

  /**
   * Creates a {@code KeyPair} from three byte arrays, representing {@code privateKey}
   * and {@code publicKey}. All arrays are defensively copied.
   */
  public static KeyPair createKeyPair(byte[] privateKey, byte[] publicKey) {
    return createKeyPairNoCopy(privateKey.clone(), publicKey.clone());
  }

  /**
   * Creates a {@code KeyPair} from three byte arrays, representing {@code privateKey}
   * and {@code publicKey}. Arrays are not copied.
   */
  static KeyPair createKeyPairNoCopy(byte[] privateKey, byte[] publicKey) {
    return new KeyPair(privateKey, publicKey);
  }

  /**
   * Returns a public key of this pair.
   */
  public PublicKey getPublicKey() {
    return publicKey;
  }

  /**
   * Returns a private key of this pair.
   */
  public PrivateKey getPrivateKey() {
    return privateKey;
  }
}

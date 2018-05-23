package com.exonum.binding.crypto;

/**
 * A key pair class that stores public key, private key and the seed they were generated with.
 */
public class KeyPair {

  private final byte[] seed;
  private final PublicKey publicKey;
  private final PrivateKey privateKey;

  private KeyPair(byte[] seed, byte[] privateKey, byte[] publicKey) {
    this.seed = seed;
    this.privateKey = PrivateKey.fromBytesNoCopy(privateKey);
    this.publicKey = PublicKey.fromBytesNoCopy(publicKey);
  }

  /**
   * Creates a {@code KeyPair} from three byte arrays, representing {@code seed},
   * {@code privateKey} and {@code publicKey}. All arrays are defensively copied.
   */
  public static KeyPair createKeyPair(byte[] seed, byte[] privateKey, byte[] publicKey) {
    return createKeyPairNoCopy(seed.clone(), privateKey.clone(), publicKey.clone());
  }

  /**
   * Creates a {@code KeyPair} from three byte arrays, representing {@code seed},
   * {@code privateKey} and {@code publicKey}. Arrays are not copied.
   */
  static KeyPair createKeyPairNoCopy(byte[] seed, byte[] privateKey, byte[] publicKey) {
    return new KeyPair(seed, privateKey, publicKey);
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

  /**
   * Returns a seed this key pair was generated with.
   */
  public byte[] getSeed() {
    return seed.clone();
  }
}

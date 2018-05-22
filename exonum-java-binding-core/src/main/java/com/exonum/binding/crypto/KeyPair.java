package com.exonum.binding.crypto;

public class KeyPair {

  private final byte[] seed;
  private final byte[] publicKey;
  private final byte[] privateKey;

  KeyPair(byte[] seed, byte[] privateKey, byte[] publicKey) {
    this.seed = seed;
    this.privateKey = privateKey;
    this.publicKey = publicKey;
  }

  public PublicKey getPublicKey() {
    return PublicKey.fromBytes(publicKey);
  }

  public PrivateKey getPrivateKey() {
    return PrivateKey.fromBytes(privateKey);
  }

  public byte[] getSeed() {
    return seed.clone();
  }
}

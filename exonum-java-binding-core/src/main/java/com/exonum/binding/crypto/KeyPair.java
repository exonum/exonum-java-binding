package com.exonum.binding.crypto;

import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_PUBLICKEYBYTES;
import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_SECRETKEYBYTES;
import static org.abstractj.kalium.NaCl.sodium;
import static org.abstractj.kalium.crypto.Util.checkLength;
import static org.abstractj.kalium.crypto.Util.isValid;
import static org.abstractj.kalium.crypto.Util.zeros;

public class KeyPair {

  private final byte[] seed;
  private final byte[] publicKey;
  private final byte[] privateKey;

  /**
   * Generates a secret key and a corresponding public key using a seed byte array.
   */
  KeyPair(byte[] seed) {
    checkLength(seed, CRYPTO_SIGN_ED25519_SECRETKEYBYTES);
    this.seed = seed;
    this.privateKey = zeros(CRYPTO_SIGN_ED25519_SECRETKEYBYTES);
    this.publicKey = zeros(CRYPTO_SIGN_ED25519_PUBLICKEYBYTES);
    isValid(sodium().crypto_sign_ed25519_seed_keypair(publicKey, privateKey, seed),
        "Failed to generate a key pair");
  }

  public PublicKey getPublicKey() {
    return new PublicKey(publicKey);
  }

  public PrivateKey getPrivateKey() {
    return new PrivateKey(privateKey);
  }

  public byte[] getSeed() {
    return seed.clone();
  }
}

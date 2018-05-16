package com.exonum.binding.crypto;

import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_BOX_CURVE25519XSALSA20POLY1305_PUBLICKEYBYTES;
import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_BOX_CURVE25519XSALSA20POLY1305_SECRETKEYBYTES;
import static org.abstractj.kalium.NaCl.sodium;
import static org.abstractj.kalium.crypto.Util.checkLength;
import static org.abstractj.kalium.crypto.Util.isValid;
import static org.abstractj.kalium.crypto.Util.zeros;

import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.encoders.Encoder;

public class KeyPair {

  private final byte[] seed;
  private byte[] publicKey;
  private final byte[] secretKey;

  public KeyPair(byte[] seed) {
    checkLength(seed, CRYPTO_BOX_CURVE25519XSALSA20POLY1305_SECRETKEYBYTES);
    this.seed = seed;
    this.secretKey = zeros(CRYPTO_BOX_CURVE25519XSALSA20POLY1305_SECRETKEYBYTES * 2);
    this.publicKey = zeros(CRYPTO_BOX_CURVE25519XSALSA20POLY1305_PUBLICKEYBYTES);
    isValid(sodium().crypto_sign_ed25519_seed_keypair(publicKey, secretKey, seed),
        "Failed to generate a key pair");
  }

  public KeyPair() {
    this(new Random().randomBytes(CRYPTO_BOX_CURVE25519XSALSA20POLY1305_SECRETKEYBYTES));
  }

  public KeyPair(String seed, Encoder encoder) {
    this(encoder.decode(seed));
  }

  public PublicKey getPublicKey() {
    return new PublicKey(publicKey);
  }

  public PrivateKey getPrivateKey() {
    return new PrivateKey(secretKey);
  }

  public byte[] getSeed() {
    return seed;
  }
}

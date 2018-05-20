package com.exonum.binding.crypto;

import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_SECRETKEYBYTES;
import static org.abstractj.kalium.crypto.Util.checkLength;
import static org.abstractj.kalium.encoders.Encoder.HEX;

import org.abstractj.kalium.keys.Key;

public class PrivateKey implements Key {

  private final byte[] privateKey;

  PrivateKey(byte[] privateKey) {
    this.privateKey = privateKey;
    checkLength(privateKey, CRYPTO_SIGN_ED25519_SECRETKEYBYTES);
  }

  @Override
  public byte[] toBytes() {
    return privateKey.clone();
  }

  @Override
  public String toString() {
    return HEX.encode(privateKey);
  }
}

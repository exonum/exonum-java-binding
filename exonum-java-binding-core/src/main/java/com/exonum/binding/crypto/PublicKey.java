package com.exonum.binding.crypto;

import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_PUBLICKEYBYTES;
import static org.abstractj.kalium.crypto.Util.checkLength;
import static org.abstractj.kalium.encoders.Encoder.HEX;

import org.abstractj.kalium.keys.Key;

public class PublicKey implements Key {

  private final byte[] publicKey;

  PublicKey(byte[] publicKey) {
    this.publicKey = publicKey;
    checkLength(publicKey, CRYPTO_SIGN_ED25519_PUBLICKEYBYTES);
  }

  @Override
  public byte[] toBytes() {
    return publicKey.clone();
  }

  @Override
  public String toString() {
    return HEX.encode(publicKey);
  }
}

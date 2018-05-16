package com.exonum.binding.crypto;

import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_BOX_CURVE25519XSALSA20POLY1305_SECRETKEYBYTES;
import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_BYTES;
import static org.abstractj.kalium.NaCl.sodium;
import static org.abstractj.kalium.crypto.Util.checkLength;
import static org.abstractj.kalium.crypto.Util.slice;
import static org.abstractj.kalium.encoders.Encoder.HEX;

import jnr.ffi.byref.LongLongByReference;
import org.abstractj.kalium.crypto.Util;
import org.abstractj.kalium.keys.Key;

public class PrivateKey implements Key {

  private final byte[] secretKey;

  public PrivateKey(byte[] secretKey) {
    this.secretKey = secretKey;
    checkLength(secretKey, CRYPTO_BOX_CURVE25519XSALSA20POLY1305_SECRETKEYBYTES * 2);
  }

  public byte[] sign(byte[] message) {
    byte[] signature = Util.prependZeros(CRYPTO_SIGN_ED25519_BYTES, message);
    LongLongByReference bufferLen = new LongLongByReference(0);
    sodium().crypto_sign_ed25519(signature, bufferLen, message, message.length, secretKey);
    signature = slice(signature, 0, CRYPTO_SIGN_ED25519_BYTES);
    return signature;
  }

  @Override
  public byte[] toBytes() {
    return secretKey;
  }

  @Override
  public String toString() {
    return HEX.encode(secretKey);
  }
}

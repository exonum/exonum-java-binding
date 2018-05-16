package com.exonum.binding.crypto;

import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_BOX_CURVE25519XSALSA20POLY1305_PUBLICKEYBYTES;
import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_BYTES;
import static org.abstractj.kalium.NaCl.sodium;
import static org.abstractj.kalium.crypto.Util.checkLength;
import static org.abstractj.kalium.crypto.Util.isValid;
import static org.abstractj.kalium.crypto.Util.merge;
import static org.abstractj.kalium.crypto.Util.zeros;
import static org.abstractj.kalium.encoders.Encoder.HEX;

import jnr.ffi.byref.LongLongByReference;
import org.abstractj.kalium.keys.Key;

public class PublicKey implements Key {

  private final byte[] publicKey;

  public PublicKey(byte[] publicKey) {
    this.publicKey = publicKey;
    checkLength(publicKey, CRYPTO_BOX_CURVE25519XSALSA20POLY1305_PUBLICKEYBYTES);
  }

  public boolean verify(byte[] message, byte[] signature) {
    checkLength(signature, CRYPTO_SIGN_ED25519_BYTES);
    byte[] sigAndMsg = merge(signature, message);
    byte[] buffer = zeros(sigAndMsg.length);
    LongLongByReference bufferLen = new LongLongByReference(0);

    return isValid(
        sodium()
            .crypto_sign_ed25519_open(buffer, bufferLen, sigAndMsg, sigAndMsg.length, publicKey),
        "Signature was forged or corrupted");
  }

  @Override
  public byte[] toBytes() {
    return publicKey;
  }

  @Override
  public String toString() {
    return HEX.encode(publicKey);
  }
}

package com.exonum.binding.crypto;

import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_BYTES;
import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_PUBLICKEYBYTES;
import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_SECRETKEYBYTES;
import static org.abstractj.kalium.NaCl.sodium;
import static org.abstractj.kalium.crypto.Util.checkLength;
import static org.abstractj.kalium.crypto.Util.isValid;
import static org.abstractj.kalium.crypto.Util.merge;
import static org.abstractj.kalium.crypto.Util.slice;
import static org.abstractj.kalium.crypto.Util.zeros;

import jnr.ffi.byref.LongLongByReference;
import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.crypto.Util;

public class Ed25519CryptoFunction implements CryptoFunction {

  @Override
  public KeyPair generateKeyPair(byte[] seed) {
    checkLength(seed, CRYPTO_SIGN_ED25519_SECRETKEYBYTES);
    byte[] privateKey = zeros(CRYPTO_SIGN_ED25519_SECRETKEYBYTES);
    byte[] publicKey = zeros(CRYPTO_SIGN_ED25519_PUBLICKEYBYTES);
    isValid(sodium().crypto_sign_ed25519_seed_keypair(publicKey, privateKey, seed),
        "Failed to generate a key pair");
    return new KeyPair(seed, privateKey, publicKey);
  }

  @Override
  public KeyPair generateKeyPair() {
    return generateKeyPair(new Random().randomBytes(CRYPTO_SIGN_ED25519_SECRETKEYBYTES));
  }

  @Override
  public byte[] signMessage(byte[] message, PrivateKey privateKey) {
    byte[] signature = Util.prependZeros(CRYPTO_SIGN_ED25519_BYTES, message);
    LongLongByReference bufferLen = new LongLongByReference(0);
    sodium()
        .crypto_sign_ed25519(signature, bufferLen, message, message.length, privateKey.toBytes());
    signature = slice(signature, 0, CRYPTO_SIGN_ED25519_BYTES);
    return signature;
  }

  @Override
  public boolean verify(byte[] message, byte[] signature, PublicKey publicKey) {
    checkLength(signature, CRYPTO_SIGN_ED25519_BYTES);
    byte[] sigAndMsg = merge(signature, message);
    byte[] buffer = zeros(sigAndMsg.length);
    LongLongByReference bufferLen = new LongLongByReference(0);

    return isValid(
        sodium()
            .crypto_sign_ed25519_open(
                buffer, bufferLen, sigAndMsg, sigAndMsg.length, publicKey.toBytes()),
        "Signature was forged or corrupted");
  }
}

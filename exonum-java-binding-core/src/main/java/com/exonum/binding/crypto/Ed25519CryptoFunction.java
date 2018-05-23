package com.exonum.binding.crypto;

import static com.exonum.binding.crypto.CryptoUtils.checkLength;
import static com.exonum.binding.crypto.CryptoUtils.checkReturnValueSuccess;
import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_BYTES;
import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_PUBLICKEYBYTES;
import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_SECRETKEYBYTES;
import static org.abstractj.kalium.NaCl.sodium;
import static org.abstractj.kalium.crypto.Util.merge;
import static org.abstractj.kalium.crypto.Util.slice;
import static org.abstractj.kalium.crypto.Util.zeros;

import jnr.ffi.byref.LongLongByReference;
import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.crypto.Util;

/**
 * A ED25519 public-key signature system crypto function.
 */
public enum Ed25519CryptoFunction implements CryptoFunction {

  INSTANCE;

  @Override
  public KeyPair generateKeyPair(byte[] seed) {
    if (!checkLength(seed, CRYPTO_SIGN_ED25519_SECRETKEYBYTES)) {
      throw new IllegalArgumentException("Seed byte array is either null or has invalid size");
    }
    byte[] privateKey = zeros(CRYPTO_SIGN_ED25519_SECRETKEYBYTES);
    byte[] publicKey = zeros(CRYPTO_SIGN_ED25519_PUBLICKEYBYTES);
    if (!checkReturnValueSuccess(
        sodium().crypto_sign_ed25519_seed_keypair(publicKey, privateKey, seed))) {
      throw new RuntimeException("Failed to generate a key pair");
    }
    return KeyPair.createKeyPairNoCopy(seed, privateKey, publicKey);
  }

  @Override
  public KeyPair generateKeyPair() {
    return generateKeyPair(new Random().randomBytes(CRYPTO_SIGN_ED25519_SECRETKEYBYTES));
  }

  @Override
  public byte[] signMessage(byte[] message, PrivateKey privateKey) {
    byte[] signedMessage = Util.prependZeros(CRYPTO_SIGN_ED25519_BYTES, message);
    LongLongByReference bufferLen = new LongLongByReference(0);
    sodium()
        .crypto_sign_ed25519(
            signedMessage, bufferLen, message, message.length, privateKey.toBytesNoCopy());
    signedMessage = slice(signedMessage, 0, CRYPTO_SIGN_ED25519_BYTES);
    return signedMessage;
  }

  @Override
  public boolean verify(byte[] message, byte[] signature, PublicKey publicKey) {
    if (!checkLength(signature, CRYPTO_SIGN_ED25519_BYTES)) {
      return false;
    }
    byte[] sigAndMsg = merge(signature, message);
    byte[] buffer = zeros(sigAndMsg.length);
    LongLongByReference bufferLen = new LongLongByReference(0);

    return checkReturnValueSuccess(
        sodium()
            .crypto_sign_ed25519_open(
                buffer, bufferLen, sigAndMsg, sigAndMsg.length, publicKey.toBytesNoCopy()));
  }
}

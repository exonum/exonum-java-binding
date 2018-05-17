package com.exonum.binding.crypto;

import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_BYTES;
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

public class CryptoUtils {

  /**
   * Generates a secret key and a corresponding public key using a seed byte array.
   */
  public static KeyPair generateKeyPair(byte[] seed) {
    return new KeyPair(seed);
  }

  /**
   * Generates a secret key and a corresponding public key using a random seed.
   */
  public static KeyPair generateKeyPair() {
    return generateKeyPair(new Random().randomBytes(CRYPTO_SIGN_ED25519_SECRETKEYBYTES));
  }

  /**
   * Given a private key, computes and returns a signedMessage for the previously supplied
   * 'message'.
   */
  public static byte[] signMessage(byte[] message, PrivateKey privateKey) {
    byte[] signature = Util.prependZeros(CRYPTO_SIGN_ED25519_BYTES, message);
    LongLongByReference bufferLen = new LongLongByReference(0);
    sodium()
        .crypto_sign_ed25519(signature, bufferLen, message, message.length, privateKey.toBytes());
    signature = slice(signature, 0, CRYPTO_SIGN_ED25519_BYTES);
    return signature;
  }

  /**
   * Verifies that `signature` is a valid signature for the supplied 'message'.
   */
  public static boolean verify(byte[] message, byte[] signedMessage, PublicKey publicKey) {
    checkLength(signedMessage, CRYPTO_SIGN_ED25519_BYTES);
    byte[] sigAndMsg = merge(signedMessage, message);
    byte[] buffer = zeros(sigAndMsg.length);
    LongLongByReference bufferLen = new LongLongByReference(0);

    return isValid(
        sodium()
            .crypto_sign_ed25519_open(
                buffer, bufferLen, sigAndMsg, sigAndMsg.length, publicKey.toBytes()),
        "Signature was forged or corrupted");
  }
}

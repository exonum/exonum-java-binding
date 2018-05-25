package com.exonum.binding.crypto;

import static com.exonum.binding.test.Bytes.bytes;
import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_BYTES;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.test.Bytes;
import org.junit.Test;

public class CryptoFunctionTest {

  private static final CryptoFunction CRYPTO_FUNCTION = CryptoFunctions.ed25519();

  @Test
  public void validSignatureVerificationTest() {
    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair();
    PrivateKey privateKey = keyPair.getPrivateKey();
    PublicKey publicKey = keyPair.getPublicKey();
    byte[] message = bytes("myMessage");
    byte[] signature = CRYPTO_FUNCTION.signMessage(message, privateKey);
    assertTrue(CRYPTO_FUNCTION.verify(message, signature, publicKey));
  }

  @Test
  public void invalidLengthSignatureVerificationTest() {
    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair();
    PublicKey publicKey = keyPair.getPublicKey();
    byte[] message = bytes("myMessage");
    byte[] invalidSignature = bytes("invalidLengthMessage");
    assertFalse(CRYPTO_FUNCTION.verify(message, invalidSignature, publicKey));
  }

  @Test
  public void invalidSignatureVerificationTest() {
    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair();
    PublicKey publicKey = keyPair.getPublicKey();
    byte[] message = bytes("myMessage");
    byte[] invalidSignature = Bytes.createPrefixed(message, CRYPTO_SIGN_ED25519_BYTES);
    assertFalse(CRYPTO_FUNCTION.verify(message, invalidSignature, publicKey));
  }

  @Test
  public void invalidMessageVerificationTest() {
    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair();
    PrivateKey privateKey = keyPair.getPrivateKey();
    PublicKey publicKey = keyPair.getPublicKey();
    byte[] message = bytes("myMessage");
    byte[] signature = CRYPTO_FUNCTION.signMessage(message, privateKey);
    byte[] anotherMessage = bytes("anotherMessage");
    assertFalse(CRYPTO_FUNCTION.verify(anotherMessage, signature, publicKey));
  }
}

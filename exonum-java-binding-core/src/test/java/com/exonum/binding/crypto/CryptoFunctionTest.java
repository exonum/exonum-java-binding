package com.exonum.binding.crypto;

import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_BYTES;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.test.Bytes;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CryptoFunctionTest {

  private static final CryptoFunction CRYPTO_FUNCTION = CryptoFunctions.ed25519();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void validSignatureVerificationTest() {
    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair();
    PrivateKey privateKey = keyPair.getPrivateKey();
    PublicKey publicKey = keyPair.getPublicKey();
    byte[] message = "myMessage".getBytes();
    byte[] signature = CRYPTO_FUNCTION.signMessage(message, privateKey);
    assertTrue(CRYPTO_FUNCTION.verify(message, signature, publicKey));
  }

  @Test
  public void invalidLengthSignatureVerificationTest() {
    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair();
    PublicKey publicKey = keyPair.getPublicKey();
    byte[] message = "myMessage".getBytes();
    byte[] invalidSignature = "invalidLengthMessage".getBytes();
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Invalid size");
    CRYPTO_FUNCTION.verify(message, invalidSignature, publicKey);
  }

  @Test
  public void invalidSignatureVerificationTest() {
    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair();
    PublicKey publicKey = keyPair.getPublicKey();
    byte[] message = "myMessage".getBytes();
    byte[] invalidSignature = Bytes.createPrefixed(message, CRYPTO_SIGN_ED25519_BYTES);
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Signature was forged or corrupted");
    CRYPTO_FUNCTION.verify(message, invalidSignature, publicKey);
  }
}

package com.exonum.binding.crypto;

import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_BYTES;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.test.Bytes;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CryptoTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void validSignatureVerificationTest() {
    KeyPair keyPair = new KeyPair();
    PrivateKey privateKey = keyPair.getPrivateKey();
    PublicKey publicKey = keyPair.getPublicKey();
    byte[] message = "myMessage".getBytes();
    byte[] signedMessage = privateKey.sign(message);
    assertTrue(publicKey.verify(message, signedMessage));
  }

  @Test
  public void invalidLengthSignatureVerificationTest() {
    KeyPair keyPair = new KeyPair();
    PublicKey publicKey = keyPair.getPublicKey();
    byte[] message = "myMessage".getBytes();
    byte[] invalidSignedMessage = "invalidLengthMessage".getBytes();
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Invalid size");
    publicKey.verify(message, invalidSignedMessage);
  }

  @Test
  public void invalidSignatureVerificationTest() {
    KeyPair keyPair = new KeyPair();
    PublicKey publicKey = keyPair.getPublicKey();
    byte[] message = "myMessage".getBytes();
    byte[] invalidSignedMessage = Bytes.createPrefixed(message, CRYPTO_SIGN_ED25519_BYTES);
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Signature was forged or corrupted");
    publicKey.verify(message, invalidSignedMessage);
  }
}

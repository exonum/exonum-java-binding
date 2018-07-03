/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.crypto;

import static com.exonum.binding.test.Bytes.bytes;
import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_BYTES;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.test.Bytes;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class Ed25519CryptoFunctionTest {

  private static final CryptoFunction CRYPTO_FUNCTION = CryptoFunctions.ed25519();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void generateKeyPairWithSeed() {
    byte[] seed = new byte[64];

    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair(seed);
    assertNotNull(keyPair);
  }

  @Test
  public void generateKeyPairInvalidSeedSize() {
    // Try to use a two-byte seed, must be 64 byte long
    byte[] seed = bytes(0x01, 0x02);

    expectedException.expectMessage("Seed byte array has invalid size (2), must be 64");
    expectedException.expect(IllegalArgumentException.class);
    CRYPTO_FUNCTION.generateKeyPair(seed);
  }

  @Test
  public void generateKeyPairNullSeed() {
    expectedException.expect(NullPointerException.class);
    CRYPTO_FUNCTION.generateKeyPair(null);
  }

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
  public void validSignatureEmptyMessageVerificationTest() {
    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair();
    PrivateKey privateKey = keyPair.getPrivateKey();
    PublicKey publicKey = keyPair.getPublicKey();
    byte[] emptyMessage = new byte[0];
    byte[] signature = CRYPTO_FUNCTION.signMessage(emptyMessage, privateKey);
    assertTrue(CRYPTO_FUNCTION.verify(emptyMessage, signature, publicKey));
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
  public void invalidPublicKeyVerificationTest() {
    // Generate a key pair.
    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair();
    PrivateKey privateKey = keyPair.getPrivateKey();
    byte[] message = bytes("myMessage");
    // Sign a message properly.
    byte[] signature = CRYPTO_FUNCTION.signMessage(message, privateKey);

    // Try to use another public key.
    PublicKey publicKey = CRYPTO_FUNCTION.generateKeyPair().getPublicKey();
    assertFalse(CRYPTO_FUNCTION.verify(message, signature, publicKey));
  }

  @Test
  public void invalidPublicKeyLengthVerificationTest() {
    // Generate a key pair.
    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair();
    PrivateKey privateKey = keyPair.getPrivateKey();
    byte[] message = bytes("myMessage");
    // Sign a message.
    byte[] signature = CRYPTO_FUNCTION.signMessage(message, privateKey);

    // Try to use a public key of incorrect length.
    PublicKey publicKey = PublicKey.fromHexString("abcd");
    assertFalse(CRYPTO_FUNCTION.verify(message, signature, publicKey));
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

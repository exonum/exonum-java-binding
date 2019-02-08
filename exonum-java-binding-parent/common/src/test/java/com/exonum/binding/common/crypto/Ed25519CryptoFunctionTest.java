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

package com.exonum.binding.common.crypto;

import static com.exonum.binding.common.crypto.CryptoFunctions.Ed25519.PRIVATE_KEY_BYTES;
import static com.exonum.binding.common.crypto.CryptoFunctions.Ed25519.PUBLIC_KEY_BYTES;
import static com.exonum.binding.common.crypto.CryptoFunctions.Ed25519.SEED_BYTES;
import static com.exonum.binding.common.crypto.CryptoFunctions.Ed25519.SIGNATURE_BYTES;
import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.test.Bytes;
import org.junit.jupiter.api.Test;

class Ed25519CryptoFunctionTest {

  private static final CryptoFunction CRYPTO_FUNCTION = CryptoFunctions.ed25519();

  @Test
  void generateKeyPairWithSeed() {
    byte[] seed = new byte[SEED_BYTES];

    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair(seed);
    assertNotNull(keyPair);
    assertThat(keyPair.getPrivateKey().size(), equalTo(PRIVATE_KEY_BYTES));
    assertThat(keyPair.getPublicKey().size(), equalTo(PUBLIC_KEY_BYTES));
  }

  @Test
  void generateKeyPairInvalidSeedSize() {
    // Try to use a two-byte seed, must be 32 byte long
    byte[] seed = bytes(0x01, 0x02);


    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> CRYPTO_FUNCTION.generateKeyPair(seed));
    assertEquals("Seed byte array has invalid size (2), must be "
        + SEED_BYTES, thrown.getMessage());
  }

  @Test
  void generateKeyPairNullSeed() {
    assertThrows(NullPointerException.class, () -> CRYPTO_FUNCTION.generateKeyPair(null));
  }

  @Test
  void validSignatureVerificationTest() {
    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair();
    PrivateKey privateKey = keyPair.getPrivateKey();
    PublicKey publicKey = keyPair.getPublicKey();
    byte[] message = bytes("myMessage");
    byte[] signature = CRYPTO_FUNCTION.signMessage(message, privateKey);
    assertTrue(CRYPTO_FUNCTION.verify(message, signature, publicKey));
  }

  @Test
  void validSignatureEmptyMessageVerificationTest() {
    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair();
    PrivateKey privateKey = keyPair.getPrivateKey();
    PublicKey publicKey = keyPair.getPublicKey();
    byte[] emptyMessage = new byte[0];
    byte[] signature = CRYPTO_FUNCTION.signMessage(emptyMessage, privateKey);
    assertTrue(CRYPTO_FUNCTION.verify(emptyMessage, signature, publicKey));
  }

  @Test
  void invalidLengthSignatureVerificationTest() {
    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair();
    PublicKey publicKey = keyPair.getPublicKey();
    byte[] message = bytes("myMessage");
    byte[] invalidSignature = bytes("invalidLengthMessage");
    assertFalse(CRYPTO_FUNCTION.verify(message, invalidSignature, publicKey));
  }

  @Test
  void invalidSignatureVerificationTest() {
    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair();
    PublicKey publicKey = keyPair.getPublicKey();
    byte[] message = bytes("myMessage");
    byte[] invalidSignature = Bytes.createPrefixed(message, SIGNATURE_BYTES);
    assertFalse(CRYPTO_FUNCTION.verify(message, invalidSignature, publicKey));
  }

  @Test
  void invalidPublicKeyVerificationTest() {
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
  void invalidPublicKeyLengthVerificationTest() {
    // Generate a key pair.
    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair();
    PrivateKey privateKey = keyPair.getPrivateKey();
    byte[] message = bytes("myMessage");
    // Sign a message.
    byte[] signature = CRYPTO_FUNCTION.signMessage(message, privateKey);

    // Try to use a public key of incorrect length.
    PublicKey publicKey = PublicKey.fromHexString("abcd");

    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> CRYPTO_FUNCTION.verify(message, signature, publicKey));
    assertEquals("Public key has invalid size (2), must be "
        + PUBLIC_KEY_BYTES, thrown.getMessage());
  }

  @Test
  void invalidMessageVerificationTest() {
    KeyPair keyPair = CRYPTO_FUNCTION.generateKeyPair();
    PrivateKey privateKey = keyPair.getPrivateKey();
    PublicKey publicKey = keyPair.getPublicKey();
    byte[] message = bytes("myMessage");
    byte[] signature = CRYPTO_FUNCTION.signMessage(message, privateKey);
    byte[] anotherMessage = bytes("anotherMessage");
    assertFalse(CRYPTO_FUNCTION.verify(anotherMessage, signature, publicKey));
  }
}

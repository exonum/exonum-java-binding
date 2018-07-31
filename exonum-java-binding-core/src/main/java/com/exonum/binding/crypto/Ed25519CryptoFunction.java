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

import static com.exonum.binding.crypto.Crypto.Ed25519.PRIVATE_KEY_BYTES;
import static com.exonum.binding.crypto.Crypto.Ed25519.PUBLIC_KEY_BYTES;
import static com.exonum.binding.crypto.Crypto.Ed25519.SEED_BYTES;
import static com.exonum.binding.crypto.Crypto.Ed25519.SIGNATURE_BYTES;
import static com.exonum.binding.crypto.CryptoUtils.hasLength;
import static com.exonum.binding.crypto.CryptoUtils.merge;
import static com.google.common.base.Preconditions.checkArgument;

import com.goterl.lazycode.lazysodium.LazySodiumJava;
import com.goterl.lazycode.lazysodium.SodiumJava;
import java.util.Arrays;

/**
 * A ED25519 public-key signature system crypto function.
 */
public enum Ed25519CryptoFunction implements CryptoFunction {

  INSTANCE;

  private final LazySodiumJava lazySodium = new LazySodiumJava(new SodiumJava());

  @Override
  public KeyPair generateKeyPair(byte[] seed) {
    checkArgument(hasLength(seed, SEED_BYTES),
        "Seed byte array has invalid size (%s), must be %s", seed.length, SEED_BYTES);

    byte[] publicKey = lazySodium.randomBytesBuf(PUBLIC_KEY_BYTES);
    byte[] privateKey = lazySodium.randomBytesBuf(PRIVATE_KEY_BYTES);

    if (!lazySodium.cryptoSignSeedKeypair(publicKey, privateKey, seed)) {
      throw new RuntimeException("Failed to generate a key pair");
    }
    return KeyPair.createKeyPairNoCopy(privateKey, publicKey);
  }

  @Override
  public KeyPair generateKeyPair() {
    byte[] publicKey = lazySodium.randomBytesBuf(PUBLIC_KEY_BYTES);
    byte[] privateKey = lazySodium.randomBytesBuf(PRIVATE_KEY_BYTES);

    if (!lazySodium.cryptoSignKeypair(publicKey, privateKey)) {
      throw new RuntimeException("Failed to generate a key pair");
    }
    return KeyPair.createKeyPairNoCopy(privateKey, publicKey);
  }

  @Override
  public byte[] signMessage(byte[] message, PrivateKey privateKey) {
    byte[] signedMessage = lazySodium.randomBytesBuf(SIGNATURE_BYTES + message.length);

    if (!lazySodium
        .cryptoSign(signedMessage, null, message, message.length, privateKey.toBytesNoCopy())) {
      throw new RuntimeException("Could not sign the message.");
    }
    return Arrays.copyOfRange(signedMessage, 0, SIGNATURE_BYTES);
  }

  @Override
  public boolean verify(byte[] message, byte[] signature, PublicKey publicKey) {
    if (!hasLength(signature, SIGNATURE_BYTES)) {
      return false;
    }
    byte[] signedMessage = merge(signature, message);

    return lazySodium.cryptoSignOpen(
        message,
        null,
        signedMessage,
        signedMessage.length,
        publicKey.toBytesNoCopy()
    );
  }

}

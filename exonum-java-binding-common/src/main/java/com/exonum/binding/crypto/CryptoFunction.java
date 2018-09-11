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

/**
 * A crypto function that provides several signature system crypto methods.
 * All method arguments can't be null otherwise {@link NullPointerException} will be thrown.
 */
public interface CryptoFunction {

  /**
   * Generates a private key and a corresponding public key using a {@code seed} byte array.
   *
   * @throws IllegalArgumentException if the specified seed is not valid
   */
  KeyPair generateKeyPair(byte[] seed);

  /**
   * Generates a private key and a corresponding public key using a random seed.
   */
  KeyPair generateKeyPair();

  /**
   * Given a {@code privateKey}, computes and returns a signature for the supplied {@code message}.
   *
   * @return signature as a byte array
   * @throws IllegalArgumentException if the private key is not valid for this cryptographic
   *     function
   */
  byte[] signMessage(byte[] message, PrivateKey privateKey);

  /**
   * Given a {@code publicKey}, verifies that {@code signature} is a valid signature for the
   * supplied {@code message}.
   *
   * @return true if signature is valid, false otherwise
   */
  boolean verify(byte[] message, byte[] signature, PublicKey publicKey);
}

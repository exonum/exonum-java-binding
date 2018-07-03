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
 * A key pair class that stores public and private keys.
 */
public class KeyPair {

  private final PublicKey publicKey;
  private final PrivateKey privateKey;

  private KeyPair(byte[] privateKey, byte[] publicKey) {
    this.privateKey = PrivateKey.fromBytesNoCopy(privateKey);
    this.publicKey = PublicKey.fromBytesNoCopy(publicKey);
  }

  /**
   * Creates a {@code KeyPair} from two byte arrays, representing {@code privateKey}
   * and {@code publicKey}. All arrays are defensively copied.
   */
  public static KeyPair createKeyPair(byte[] privateKey, byte[] publicKey) {
    return createKeyPairNoCopy(privateKey.clone(), publicKey.clone());
  }

  /**
   * Creates a {@code KeyPair} from two byte arrays, representing {@code privateKey}
   * and {@code publicKey}. Arrays are not copied.
   */
  static KeyPair createKeyPairNoCopy(byte[] privateKey, byte[] publicKey) {
    return new KeyPair(privateKey, publicKey);
  }

  /**
   * Returns a public key of this pair.
   */
  public PublicKey getPublicKey() {
    return publicKey;
  }

  /**
   * Returns a private key of this pair.
   */
  public PrivateKey getPrivateKey() {
    return privateKey;
  }
}

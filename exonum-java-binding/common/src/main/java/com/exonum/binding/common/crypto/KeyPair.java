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

/**
 * A key pair class that stores public and private keys.
 */
public class KeyPair {

  private final PrivateKey privateKey;
  private final PublicKey publicKey;

  private KeyPair(PrivateKey privateKey, PublicKey publicKey) {
    this.privateKey = privateKey;
    this.publicKey = publicKey;
  }

  /**
   * Creates a new KeyPair from the given key pairs.
   */
  public static KeyPair newInstance(PrivateKey privateKey, PublicKey publicKey) {
    return new KeyPair(privateKey, publicKey);
  }

  /**
   * Creates a {@code KeyPair} from two byte arrays, representing {@code privateKey}
   * and {@code publicKey}. All arrays are defensively copied.
   */
  public static KeyPair newInstance(byte[] privateKey, byte[] publicKey) {
    return newInstanceNoCopy(privateKey.clone(), publicKey.clone());
  }

  /**
   * Creates a {@code KeyPair} from two byte arrays, representing {@code privateKey}
   * and {@code publicKey}. Arrays are not copied.
   */
  static KeyPair newInstanceNoCopy(byte[] privateKey, byte[] publicKey) {
    return new KeyPair(PrivateKey.fromBytesNoCopy(privateKey),
        PublicKey.fromBytesNoCopy(publicKey));
  }

  /**
   * Returns a private key of this pair.
   */
  public PrivateKey getPrivateKey() {
    return privateKey;
  }

  /**
   * Returns a public key of this pair.
   */
  public PublicKey getPublicKey() {
    return publicKey;
  }
}

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

import static org.abstractj.kalium.encoders.Encoder.HEX;

/**
 * Represent a public key in a digital signature system.
 */
public final class PublicKey extends AbstractKey {

  private PublicKey(byte[] publicKey) {
    super(publicKey);
  }

  /**
   * Creates a {@code PublicKey} from a byte array. The array is defensively copied.
   */
  public static PublicKey fromBytes(byte[] bytes) {
    return fromBytesNoCopy(bytes.clone());
  }

  /**
   * Creates a {@code PublicKey} from a byte array. The array is not copied defensively.
   */
  static PublicKey fromBytesNoCopy(byte[] bytes) {
    return new PublicKey(bytes);
  }

  /**
   * Creates a {@code PublicKey} from a hexadecimal string.
   */
  public static PublicKey fromHexString(String stringKey) {
    return new PublicKey(HEX.decode(stringKey));
  }
}

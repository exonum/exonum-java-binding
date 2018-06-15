/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
 * Represent a private key in a digital signature system.
 */
public final class PrivateKey extends AbstractKey {

  private PrivateKey(byte[] privateKey) {
    super(privateKey);
  }

  /**
   * Creates a {@code PrivateKey} from a byte array. The array is defensively copied.
   */
  public static PrivateKey fromBytes(byte[] bytes) {
    return fromBytesNoCopy(bytes.clone());
  }

  /**
   * Creates a {@code PrivateKey} from a byte array. The array is not copied defensively.
   */
  static PrivateKey fromBytesNoCopy(byte[] bytes) {
    return new PrivateKey(bytes);
  }

  /**
   * Creates a {@code PrivateKey} from a hexadecimal string.
   */
  public static PrivateKey fromHexString(String stringKey) {
    return new PrivateKey(HEX.decode(stringKey));
  }
}

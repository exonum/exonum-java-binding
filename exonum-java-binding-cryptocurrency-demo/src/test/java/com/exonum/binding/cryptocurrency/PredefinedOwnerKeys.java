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

package com.exonum.binding.cryptocurrency;

import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SIGN_ED25519_PUBLICKEYBYTES;

import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.test.Bytes;

public class PredefinedOwnerKeys {

  public static final PublicKey firstOwnerKey =
      PublicKey.fromBytes(Bytes.createPrefixed(Bytes.bytes(0), CRYPTO_SIGN_ED25519_PUBLICKEYBYTES));

  public static final PublicKey secondOwnerKey =
      PublicKey.fromBytes(Bytes.createPrefixed(Bytes.bytes(1), CRYPTO_SIGN_ED25519_PUBLICKEYBYTES));

  private PredefinedOwnerKeys() {}
}

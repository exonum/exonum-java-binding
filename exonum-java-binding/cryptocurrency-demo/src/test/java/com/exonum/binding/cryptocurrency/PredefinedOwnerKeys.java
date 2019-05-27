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

import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.KeyPair;

public class PredefinedOwnerKeys {

  private static final CryptoFunction CRYPTO_FUNCTION = CryptoFunctions.ed25519();

  public static final KeyPair FIRST_OWNER_KEY_PAIR = CRYPTO_FUNCTION.generateKeyPair();
  public static final KeyPair SECOND_OWNER_KEY_PAIR = CRYPTO_FUNCTION.generateKeyPair();

  private PredefinedOwnerKeys() {
    throw new AssertionError("Non-instantiable");
  }
}

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

import static com.exonum.binding.common.crypto.CryptoFunctions.Ed25519.PUBLIC_KEY_BYTES;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.test.Bytes;

public class PredefinedOwnerKeys {

  public static final PublicKey FIRST_OWNER_KEY =
      PublicKey.fromBytes(Bytes.createPrefixed(Bytes.bytes(0), PUBLIC_KEY_BYTES));

  public static final PublicKey SECOND_OWNER_KEY =
      PublicKey.fromBytes(Bytes.createPrefixed(Bytes.bytes(1), PUBLIC_KEY_BYTES));

  private PredefinedOwnerKeys() {
    throw new AssertionError("Non-instantiable");
  }
}

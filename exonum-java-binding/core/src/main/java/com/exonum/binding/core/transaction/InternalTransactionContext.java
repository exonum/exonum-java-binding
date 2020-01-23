/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.core.transaction;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.storage.database.Fork;
import com.google.auto.value.AutoValue;

/** Default implementation of the transaction context. */
@AutoValue
abstract class InternalTransactionContext implements TransactionContext {

  public static InternalTransactionContext newInstance(
      Fork fork, HashCode hash, PublicKey authorPk, String serviceName, int serviceId) {
    return new AutoValue_InternalTransactionContext(fork, hash, authorPk, serviceName, serviceId);
  }
}

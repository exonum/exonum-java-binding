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

package com.exonum.binding.cryptocurrency.transactions;

import static com.google.common.base.Preconditions.checkArgument;

final class TransactionPreconditions {

  private TransactionPreconditions() {
    throw new AssertionError("Non-instantiable");
  }

  static void checkTransactionId(int txId, int expectedTxId) {
    checkArgument(
        txId == expectedTxId,
        "Transaction has wrong transaction ID (%s), must be %s",
        txId,
        expectedTxId);
  }
}

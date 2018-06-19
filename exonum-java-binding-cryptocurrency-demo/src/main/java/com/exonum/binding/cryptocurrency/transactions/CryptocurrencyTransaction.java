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

package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.messages.Transaction;
import com.google.common.primitives.Shorts;

public enum CryptocurrencyTransaction {
  CREATE_WALLET(1, CreateWalletTx.class),
  TRANSFER(2, TransferTx.class);

  private final short id;
  private final Class<? extends Transaction> transactionClass;

  CryptocurrencyTransaction(int id, Class<? extends Transaction> transactionClass) {
    this.id = Shorts.checkedCast(id);
    this.transactionClass = transactionClass;
  }

  public short getId() {
    return id;
  }

  public Class<? extends Transaction> transactionClass() {
    return transactionClass;
  }
}

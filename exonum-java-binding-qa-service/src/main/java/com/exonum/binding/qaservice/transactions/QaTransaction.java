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

package com.exonum.binding.qaservice.transactions;

import com.google.common.primitives.Shorts;


/**
 * All known QA service transactions.
 *
 * @implNote Keep in sync with {@link QaTransactionConverter#TRANSACTION_FACTORIES}.
 */
public enum QaTransaction {
  // Well-behaved transactions.
  CREATE_COUNTER(0),
  INCREMENT_COUNTER(1),

  // Badly-behaved transactions, do some crazy things.
  INVALID(10),
  INVALID_THROWING(11),
  VALID_THROWING(12);

  private final short id;

  QaTransaction(int id) {
    this.id = Shorts.checkedCast(id);
  }

  /** Returns the unique id of this transaction. */
  public short id() {
    return id;
  }

}

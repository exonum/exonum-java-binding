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

package com.exonum.binding.qaservice.transactions;

import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.transaction.AbstractTransaction;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.TransactionContext;

/**
 * A transaction that has QA service identifier, but an unknown transaction id.
 * Such transaction must be rejected when received by other nodes.
 *
 * <p>Only a single unknown transaction may be submitted to each node,
 * as they have empty body (= the same binary representation),
 * and once it is added to the local pool of a certain node,
 * it will remain there. Other nodes must reject the message of this transaction
 * once they receive it as a message from this node. If multiple unknown transaction messages
 * need to be submitted, a seed might be added.
 */
public final class UnknownTx extends AbstractTransaction {

  static final short ID = 9999;

  public UnknownTx() {
    super(createRawTransaction());
  }

  @Override
  public void execute(TransactionContext context) {
    throw new AssertionError("Must never be executed by the framework: " + this);
  }

  public static RawTransaction createRawTransaction() {
    return RawTransaction.newBuilder()
        .serviceId(QaService.ID)
        .transactionId(ID)
        .payload(new byte[]{})
        .build();
  }
}

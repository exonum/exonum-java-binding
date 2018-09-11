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

import static com.exonum.binding.qaservice.transactions.QaTransactionTemplate.newQaTransactionBuilder;
import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkMessageSize;
import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkTransaction;

import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.transaction.Transaction;
import java.util.Collections;
import java.util.Map;

/**
 * An invalid transaction always throwing IllegalStateException in {@link #isValid()}.
 */
public final class InvalidThrowingTx implements Transaction {

  private static final short ID = QaTransaction.INVALID_THROWING.id();
  private static final String INVALID_TX_JSON = QaTransactionGson.instance()
      .toJson(new AnyTransaction<Map>(ID, Collections.emptyMap()));

  @Override
  public boolean isValid() {
    throw new IllegalStateException("#isValid of this transaction always throws: " + this);
  }

  @Override
  public void execute(Fork view) {
    throw new AssertionError("Must never be executed by the framework: " + this);
  }

  @Override
  public String info() {
    return INVALID_TX_JSON;
  }

  @Override
  public BinaryMessage getMessage() {
    return converter().toMessage(this);
  }

  static TransactionMessageConverter<InvalidThrowingTx> converter() {
    return TransactionConverter.INSTANCE;
  }

  private enum TransactionConverter implements TransactionMessageConverter<InvalidThrowingTx> {
    INSTANCE;

    static final int BODY_SIZE = 0;

    @Override
    public InvalidThrowingTx fromMessage(Message txMessage) {
      checkMessage(txMessage);
      return new InvalidThrowingTx();
    }

    @Override
    public BinaryMessage toMessage(InvalidThrowingTx transaction) {
      return newQaTransactionBuilder(ID)
          .buildRaw();
    }

    private void checkMessage(Message txMessage) {
      checkTransaction(txMessage, ID);
      checkMessageSize(txMessage, BODY_SIZE);
    }
  }
}

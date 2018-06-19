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

import static com.exonum.binding.qaservice.transactions.QaTransactionTemplate.newQaTransactionBuilder;
import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkMessageSize;
import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkTransaction;

import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.storage.database.Fork;
import java.util.Collections;
import java.util.Map;

/**
 * An invalid transaction always returning false in {@link #isValid()}.
 */
public final class InvalidTx implements Transaction {

  private static final short ID = QaTransaction.INVALID.id();
  private static final String INVALID_TX_JSON = QaTransactionGson.instance()
      .toJson(new AnyTransaction<Map>(ID, Collections.emptyMap()));

  @Override
  public boolean isValid() {
    return false;
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

  static TransactionMessageConverter<InvalidTx> converter() {
    return TransactionConverter.INSTANCE;
  }

  private enum TransactionConverter implements TransactionMessageConverter<InvalidTx> {
    INSTANCE;

    static final int BODY_SIZE = 0;

    @Override
    public InvalidTx fromMessage(Message txMessage) {
      checkMessage(txMessage);
      return new InvalidTx();
    }

    @Override
    public BinaryMessage toMessage(InvalidTx transaction) {
      return newQaTransactionBuilder(ID)
          .buildRaw();
    }

    private void checkMessage(Message txMessage) {
      checkTransaction(txMessage, ID);
      checkMessageSize(txMessage, BODY_SIZE);
    }
  }
}

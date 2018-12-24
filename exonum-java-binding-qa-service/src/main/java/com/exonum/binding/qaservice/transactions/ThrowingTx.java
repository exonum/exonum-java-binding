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

import static com.exonum.binding.common.serialization.StandardSerializers.protobuf;
import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkTransaction;

import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.qaservice.transactions.TxMessageProtos.ThrowingTxBody;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
import com.exonum.binding.transaction.TransactionContext;
import java.util.Objects;

public final class ThrowingTx implements Transaction {

  private static final short ID = QaTransaction.VALID_THROWING.id();
  private static final Serializer<ThrowingTxBody> PROTO_SERIALIZER =
      protobuf(ThrowingTxBody.class);

  private final long seed;

  public ThrowingTx(long seed) {
    this.seed = seed;
  }

  /**
   * First clears the indices of the service, then throws an exception.
   *
   * @throws IllegalStateException always
   */
  @Override
  public void execute(TransactionContext context) {
    QaSchema schema = new QaSchema(context.getFork());

    // Attempt to clear all service indices.
    schema.clearAll();

    // Throw an exception. Framework must revert the changes made above.
    throw new IllegalStateException("#execute of this transaction always throws: " + this);
  }

  @Override
  public String info() {
    return QaTransactionJson.toJson(ID, this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ThrowingTx that = (ThrowingTx) o;
    return seed == that.seed;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(seed);
  }

  public static BiDirectionTransactionConverter<ThrowingTx> converter() {
    return Converter.INSTANCE;
  }

  private enum Converter implements BiDirectionTransactionConverter<ThrowingTx> {
    INSTANCE;

    @Override
    public ThrowingTx fromRawTransaction(RawTransaction rawTransaction) {
      checkRawTransaction(rawTransaction);

      long seed = PROTO_SERIALIZER.fromBytes(rawTransaction.getPayload())
          .getSeed();
      return new ThrowingTx(seed);
    }

    @Override
    public RawTransaction toRawTransaction(ThrowingTx transaction) {
      byte[] payload = PROTO_SERIALIZER.toBytes(ThrowingTxBody.newBuilder()
          .setSeed(transaction.seed)
          .build());

      return RawTransaction.newBuilder()
          .serviceId(QaService.ID)
          .transactionId(ID)
          .payload(payload)
          .build();
    }

    private void checkRawTransaction(RawTransaction rawTransaction) {
      checkTransaction(rawTransaction, ID);
    }
  }

}

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

import static com.exonum.binding.common.hash.Hashing.DEFAULT_HASH_SIZE_BITS;
import static com.exonum.binding.qaservice.transactions.TransactionError.UNKNOWN_COUNTER;
import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkTransaction;
import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.core.transaction.RawTransaction;
import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transaction.TransactionExecutionException;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.qaservice.transactions.TxMessageProtos.IncrementCounterTxBody;
import com.google.protobuf.ByteString;
import java.util.Objects;

/**
 * A transaction incrementing the given counter. Always valid, does nothing if the counter
 * is unknown.
 */
public final class IncrementCounterTx implements Transaction {

  private static final short ID = QaTransaction.INCREMENT_COUNTER.id();
  private static final Serializer<IncrementCounterTxBody> PROTO_SERIALIZER =
      StandardSerializers.protobuf(IncrementCounterTxBody.class);

  private final long seed;
  private final HashCode counterId;

  /**
   * Creates a new increment counter transaction.
   *
   * @param seed transaction seed
   * @param counterId counter id, a hash of the counter name
   */
  public IncrementCounterTx(long seed, HashCode counterId) {
    int size = counterId.bits();
    checkArgument(size == DEFAULT_HASH_SIZE_BITS,
        "Counter [%s] has %s bits size but required size is %s",
        counterId, size, DEFAULT_HASH_SIZE_BITS);
    this.seed = seed;
    this.counterId = counterId;
  }

  @Override
  public void execute(TransactionContext context) throws TransactionExecutionException {
    QaSchema schema = new QaSchema(context.getFork());
    ProofMapIndexProxy<HashCode, Long> counters = schema.counters();

    // Increment the counter if there is such.
    if (!counters.containsKey(counterId)) {
      throw new TransactionExecutionException(UNKNOWN_COUNTER.code);
    }
    long newValue = counters.get(counterId) + 1;
    counters.put(counterId, newValue);
  }

  @Override
  public String info() {
    return QaTransactionJson.toJson(ID, this);
  }

  public RawTransaction toRawTransaction() {
    return converter().toRawTransaction(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IncrementCounterTx that = (IncrementCounterTx) o;
    return seed == that.seed
        && Objects.equals(counterId, that.counterId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(seed, counterId);
  }

  public static BiDirectionTransactionConverter<IncrementCounterTx> converter() {
    return Converter.INSTANCE;
  }

  private enum Converter implements BiDirectionTransactionConverter<IncrementCounterTx> {
    INSTANCE;

    @Override
    public IncrementCounterTx fromRawTransaction(RawTransaction rawTransaction) {
      checkMessage(rawTransaction);

      IncrementCounterTxBody body = PROTO_SERIALIZER.fromBytes(rawTransaction.getPayload());
      long seed = body.getSeed();
      byte[] rawCounterId = body.getCounterId().toByteArray();
      HashCode counterId = HashCode.fromBytes(rawCounterId);

      return new IncrementCounterTx(seed, counterId);
    }

    @Override
    public RawTransaction toRawTransaction(IncrementCounterTx transaction) {
      byte[] payload = PROTO_SERIALIZER.toBytes(IncrementCounterTxBody.newBuilder()
          .setSeed(transaction.seed)
          .setCounterId(ByteString.copyFrom(transaction.counterId.asBytes()))
          .build());

      return RawTransaction.newBuilder()
          .serviceId(QaService.ID)
          .transactionId(ID)
          .payload(payload)
          .build();
    }

    private void checkMessage(RawTransaction rawTransaction) {
      checkTransaction(rawTransaction, ID);
    }
  }

}

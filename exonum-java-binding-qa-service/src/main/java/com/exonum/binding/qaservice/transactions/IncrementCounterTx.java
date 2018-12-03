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

import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkTransaction;
import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.qaservice.transactions.TxMessageProtos.IncrementCounterTxBody;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
import com.exonum.binding.transaction.TransactionContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * A transaction incrementing the given counter. Always valid, does nothing if the counter
 * is unknown.
 */
public final class IncrementCounterTx implements Transaction {

  private static final short ID = QaTransaction.INCREMENT_COUNTER.id();

  private final long seed;
  private final HashCode counterId;

  /**
   * Creates a new increment counter transaction.
   *
   * @param seed transaction seed
   * @param counterId counter id, a hash of the counter name
   */
  public IncrementCounterTx(long seed, HashCode counterId) {
    checkArgument(counterId.bits() == Hashing.DEFAULT_HASH_SIZE_BITS);
    this.seed = seed;
    this.counterId = counterId;
  }

  @Override
  public void execute(TransactionContext context) {
    QaSchema schema = new QaSchema(context.getFork());
    ProofMapIndexProxy<HashCode, Long> counters = schema.counters();
    // Increment the counter if there is such.
    if (counters.containsKey(counterId)) {
      long newValue = counters.get(counterId) + 1;
      counters.put(counterId, newValue);
    }
  }

  @Override
  public RawTransaction getRawTransaction() {
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

  static TransactionMessageConverter<IncrementCounterTx> converter() {
    return MessageConverter.INSTANCE;
  }

  private enum MessageConverter implements TransactionMessageConverter<IncrementCounterTx> {
    INSTANCE;

    @Override
    public IncrementCounterTx fromRawTransaction(RawTransaction rawTransaction) {
      checkMessage(rawTransaction);

      // Unpack the message.
      ByteBuffer rawBody = ByteBuffer.wrap(rawTransaction.getPayload());
      try {
        IncrementCounterTxBody body = IncrementCounterTxBody.parseFrom(rawBody);
        long seed = body.getSeed();
        byte[] rawCounterId = body.getCounterId().toByteArray();
        HashCode counterId = HashCode.fromBytes(rawCounterId);

        return new IncrementCounterTx(seed, counterId);
      } catch (InvalidProtocolBufferException e) {
        throw new IllegalArgumentException(e);
      }
    }

    @Override
    public RawTransaction toRawTransaction(IncrementCounterTx transaction) {
      return transaction.getRawTransaction();
    }

    private void checkMessage(RawTransaction rawTransaction) {
      checkTransaction(rawTransaction, ID);
    }
  }

  @VisibleForTesting
  static byte[] serializeBody(IncrementCounterTx transaction) {
    return IncrementCounterTxBody.newBuilder()
        .setSeed(transaction.seed)
        .setCounterId(toByteString(transaction.counterId))
        .build()
        .toByteArray();
  }

  private static ByteString toByteString(HashCode hash) {
    return ByteString.copyFrom(hash.asBytes());
  }
}

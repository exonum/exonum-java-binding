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
import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkTransaction;
import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.qaservice.transactions.TxMessageProtos.IncrementCounterTxBody;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
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
  public boolean isValid() {
    return true;
  }

  @Override
  public void execute(Fork view) {
    QaSchema schema = new QaSchema(view);
    ProofMapIndexProxy<HashCode, Long> counters = schema.counters();
    // Increment the counter if there is such.
    if (counters.containsKey(counterId)) {
      long newValue = counters.get(counterId) + 1;
      counters.put(counterId, newValue);
    }
  }

  @Override
  public String info() {
    return new QaTransactionGson().toJson(ID, this);
  }

  @Override
  public BinaryMessage getMessage() {
    return converter().toMessage(this);
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
    public IncrementCounterTx fromMessage(Message message) {
      checkMessage(message);

      // Unpack the message.
      ByteBuffer rawBody = message.getBody();
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
    public BinaryMessage toMessage(IncrementCounterTx transaction) {
      return newQaTransactionBuilder(ID)
          .setBody(serializeBody(transaction))
          .buildRaw();
    }

    private void checkMessage(Message message) {
      checkTransaction(message, ID);
    }
  }

  @VisibleForTesting
  static ByteBuffer serializeBody(IncrementCounterTx transaction) {
    IncrementCounterTxBody txBody = IncrementCounterTxBody.newBuilder()
        .setSeed(transaction.seed)
        .setCounterId(toByteString(transaction.counterId))
        .build();
    return ByteBuffer.wrap(txBody.toByteArray());
  }

  private static ByteString toByteString(HashCode hash) {
    return ByteString.copyFrom(hash.asBytes());
  }
}

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
import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkTransaction;
import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.serialization.StandardSerializers;
import com.google.common.base.Objects;
import java.nio.ByteBuffer;

/**
 * A transaction creating a new named counter.
 */
public final class CreateCounterTx implements Transaction {

  private static final short ID = QaTransaction.CREATE_COUNTER.id();

  private final String name;

  public CreateCounterTx(String name) {
    checkArgument(!name.trim().isEmpty(), "Name must not be blank: '%s'", name);
    this.name = name;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public void execute(Fork view) {
    QaSchema schema = new QaSchema(view);
    MapIndex<HashCode, Long> counters = schema.counters();
    MapIndex<HashCode, String> names = schema.counterNames();

    HashCode counterId = Hashing.defaultHashFunction()
        .hashString(name, UTF_8);
    if (counters.containsKey(counterId)) {
      return;
    }
    assert !names.containsKey(counterId) : "counterNames must not contain the id of " + name;

    counters.put(counterId, 0L);
    names.put(counterId, name);
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
    CreateCounterTx that = (CreateCounterTx) o;
    return Objects.equal(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }

  static TransactionMessageConverter<CreateCounterTx> converter() {
    return TransactionConverter.INSTANCE;
  }

  private enum TransactionConverter implements TransactionMessageConverter<CreateCounterTx> {
    INSTANCE;

    @Override
    public CreateCounterTx fromMessage(Message txMessage) {
      checkTransaction(txMessage, ID);
      ByteBuffer body = txMessage.getBody();
      String name = getUtf8String(body);
      return new CreateCounterTx(name);
    }

    private static String getUtf8String(ByteBuffer buffer) {
      byte[] s = getRemainingBytes(buffer);

      return StandardSerializers.string()
          .fromBytes(s);
    }

    private static byte[] getRemainingBytes(ByteBuffer buffer) {
      int numBytes = buffer.remaining();
      byte[] dst = new byte[numBytes];
      buffer.get(dst);
      return dst;
    }

    @Override
    public BinaryMessage toMessage(CreateCounterTx transaction) {
      return newQaTransactionBuilder(ID)
          .setBody(serialize(transaction))
          .buildRaw();
    }

    private static ByteBuffer serialize(CreateCounterTx tx) {
      byte[] nameBytes = StandardSerializers.string().toBytes(tx.name);
      return ByteBuffer.wrap(nameBytes);
    }
  }
}

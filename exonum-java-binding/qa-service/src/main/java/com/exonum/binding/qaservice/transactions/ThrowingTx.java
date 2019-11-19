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

import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.qaservice.transactions.TxMessageProtos.ThrowingTxBody;
import java.util.Objects;

public final class ThrowingTx implements Transaction {

  private static final Serializer<ThrowingTxBody> PROTO_SERIALIZER =
      protobuf(ThrowingTxBody.class);

  private final long seed;

  ThrowingTx(long seed) {
    this.seed = seed;
  }

  static ThrowingTx fromBytes(byte[] bytes) {
    long seed = PROTO_SERIALIZER.fromBytes(bytes).getSeed();
    return new ThrowingTx(seed);
  }

  /**
   * First clears the indices of the service, then throws an exception.
   *
   * @throws IllegalStateException always
   */
  @Override
  public void execute(TransactionContext context) {
    QaSchema schema = new QaSchema(context.getFork(), context.getServiceName());

    // Attempt to clear all service indices.
    schema.clearAll();

    // Throw an exception. Framework must revert the changes made above.
    throw new IllegalStateException("#execute of this transaction always throws: " + this);
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
}

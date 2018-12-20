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

package com.exonum.binding.cryptocurrency.transactions;

import static com.exonum.binding.common.serialization.StandardSerializers.protobuf;
import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static com.exonum.binding.cryptocurrency.transactions.TransactionError.WALLET_ALREADY_EXISTS;
import static com.exonum.binding.cryptocurrency.transactions.TransactionPreconditions.checkTransaction;
import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.cryptocurrency.CryptocurrencySchema;
import com.exonum.binding.cryptocurrency.Wallet;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
import com.exonum.binding.transaction.TransactionContext;
import com.exonum.binding.transaction.TransactionExecutionException;
import com.google.common.annotations.VisibleForTesting;
import java.util.Objects;

/** A transaction that creates a new named wallet with default balance. */
public final class CreateWalletTx implements Transaction {

  static final short ID = 1;
  private static final Serializer<TxMessageProtos.CreateWalletTx> PROTO_SERIALIZER =
      protobuf(TxMessageProtos.CreateWalletTx.class);
  private final long initialBalance;

  @VisibleForTesting
  CreateWalletTx(long initialBalance) {
    checkArgument(initialBalance >= 0, "The initial balance (%s) must not be negative.",
        initialBalance);

    this.initialBalance = initialBalance;
  }

  /**
   * Creates a create wallet transaction from the serialized transaction data.
   * @param rawTransaction a raw transaction
   */
  public static CreateWalletTx fromRawTransaction(RawTransaction rawTransaction) {
    checkTransaction(rawTransaction, ID);

    TxMessageProtos.CreateWalletTx body =
        PROTO_SERIALIZER.fromBytes(rawTransaction.getPayload());

    long initialBalance = body.getInitialBalance();
    return new CreateWalletTx( initialBalance);
  }

  @Override
  public void execute(TransactionContext context) throws TransactionExecutionException {
    PublicKey ownerPublicKey = context.getAuthorPk();

    CryptocurrencySchema schema = new CryptocurrencySchema(context.getFork());
    MapIndex<PublicKey, Wallet> wallets = schema.wallets();

    if (wallets.containsKey(ownerPublicKey)) {
      throw new TransactionExecutionException(WALLET_ALREADY_EXISTS.errorCode);
    }

    Wallet wallet = new Wallet(initialBalance);

    wallets.put(ownerPublicKey, wallet);
  }

  @Override
  public String info() {
    return json().toJson(this);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateWalletTx that = (CreateWalletTx) o;
    return initialBalance == that.initialBalance;
  }

  @Override
  public int hashCode() {
    return Objects.hash(initialBalance);
  }
}


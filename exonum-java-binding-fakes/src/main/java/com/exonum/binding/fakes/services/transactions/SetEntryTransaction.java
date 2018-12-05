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

package com.exonum.binding.fakes.services.transactions;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.EntryIndexProxy;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
import com.exonum.binding.transaction.TransactionContext;

/**
 * A transaction whose behaviour can be configured. It's not a mock: it writes a given value
 * into the database in its {@link #execute(TransactionContext)}.
 *
 * <p>Such transaction is supposed to be used in TransactionProxy integration tests.
 */
public final class SetEntryTransaction implements Transaction {

  static final String ENTRY_NAME = "test_entry";
  static final String TEST_ENTRY_NAME = "test_entry";
  static final String TX_HASH_NAME = "tx_hash";
  static final String AUTHOR_PK_NAME = "author_pk";

  private final String value;

  /**
   * Creates a transaction with a pre-configured behaviour.
   *
   * @param value a value to put into an entry {@link #TEST_ENTRY_NAME}
   */
  public SetEntryTransaction(String value) {
    this.value = checkNotNull(value);
  }

  @Override
  public void execute(TransactionContext context) {
    Fork fork = context.getFork();
    EntryIndexProxy<String> entry = createTestEntry(context.getFork());
    entry.set(value);
    EntryIndexProxy<HashCode> txHash = createTxHashEntry(fork);
    txHash.set(context.getTransactionMessageHash());
    EntryIndexProxy<PublicKey> authorPk = createAuthorPkEntry(fork);
    authorPk.set(context.getAuthorPk());
  }

  @Override
  public RawTransaction getRawTransaction() {
    // Not needed for transaction ITs.
    throw new UnsupportedOperationException("Transaction#getRawTransaction is not implemented");
  }

  private EntryIndexProxy<String> createTestEntry(Fork view) {
    return EntryIndexProxy.newInstance(TEST_ENTRY_NAME, view, StandardSerializers.string());
  }

  private EntryIndexProxy<HashCode> createTxHashEntry(Fork view) {
    return EntryIndexProxy.newInstance(TX_HASH_NAME, view, StandardSerializers.hash());
  }

  private EntryIndexProxy<PublicKey> createAuthorPkEntry(Fork view) {
    return EntryIndexProxy.newInstance(AUTHOR_PK_NAME, view, StandardSerializers.publicKey());
  }
}

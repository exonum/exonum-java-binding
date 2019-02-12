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

import static com.exonum.binding.common.serialization.StandardSerializers.hash;
import static com.exonum.binding.common.serialization.StandardSerializers.publicKey;
import static com.exonum.binding.common.serialization.StandardSerializers.string;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.indices.EntryIndexProxy;
import com.exonum.binding.transaction.Transaction;
import com.exonum.binding.transaction.TransactionContext;
import com.google.common.annotations.VisibleForTesting;

/**
 * A transaction whose behaviour can be configured. It's not a mock: it writes a given value
 * into the database in its {@link #execute(TransactionContext)}.
 *
 * <p>Such transaction is supposed to be used in TransactionProxy integration tests.
 */
public final class SetEntryTransaction implements Transaction {

  @VisibleForTesting
  static final String ENTRY_NAME = "test_entry";
  @VisibleForTesting
  static final String TX_HASH_NAME = "tx_hash";
  @VisibleForTesting
  static final String AUTHOR_PK_NAME = "author_pk";

  private final String value;
  private final String info;

  /**
   * Creates a transaction with a pre-configured behaviour.
   *
   * @param value a value to put into an entry {@link #ENTRY_NAME}
   * @param info a value to be returned as this transaction text representation
   *     {@link Transaction#info()}
   */
  public SetEntryTransaction(String value, String info) {
    this.value = checkNotNull(value);
    this.info = checkNotNull(info);
  }

  @Override
  public void execute(TransactionContext context) {
    Fork fork = context.getFork();
    EntryIndexProxy<String> entry = createTestEntry(fork);
    entry.set(value);
    EntryIndexProxy<HashCode> txHash = createTxHashEntry(fork);
    txHash.set(context.getTransactionMessageHash());
    EntryIndexProxy<PublicKey> authorPk = createAuthorPkEntry(fork);
    authorPk.set(context.getAuthorPk());
  }

  @Override
  public String info() {
    return info;
  }

  private EntryIndexProxy<String> createTestEntry(Fork view) {
    return EntryIndexProxy.newInstance(ENTRY_NAME, view, string());
  }

  private EntryIndexProxy<HashCode> createTxHashEntry(Fork view) {
    return EntryIndexProxy.newInstance(TX_HASH_NAME, view, hash());
  }

  private EntryIndexProxy<PublicKey> createAuthorPkEntry(Fork view) {
    return EntryIndexProxy.newInstance(AUTHOR_PK_NAME, view, publicKey());
  }
}

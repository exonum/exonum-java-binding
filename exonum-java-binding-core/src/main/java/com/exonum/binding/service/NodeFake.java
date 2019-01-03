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

package com.exonum.binding.service;

import static com.exonum.binding.common.hash.Hashing.DEFAULT_HASH_SIZE_BYTES;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.transaction.RawTransaction;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An implementation of a Node interface for testing purposes.
 * Use it in tests of your handlers that need some data in the database:
 *
 * <pre><code>
 * try (MemoryDb db = MemoryDb.newInstance();
 *      Cleaner cleaner = new Cleaner()) {
 *
 *   // Setup database to include some test data
 *   Fork fork = db.createFork(cleaner);
 *   MapIndex balance = MapIndexProxy.newInstance("balance", fork, stringSerializer,
 *       stringSerializer);
 *   balance.put("John Doe", "$1000.00");
 *   db.merge(fork);
 *
 *   // Create a node fake from the database
 *   NodeFake node = new NodeFake(db);
 *
 *   WalletController controller = new WalletController(node);
 *
 *   assertThat(controller.getBalance("John Doe"), equalTo("$1000.00"));
 * }
 * </code></pre>
 */
public final class NodeFake implements Node {

  private static final Logger logger = LogManager.getLogger(NodeFake.class);

  private final MemoryDb database;

  private final byte[] publicKey;

  /**
   * Creates a new node fake with the given database and an empty public key.
   *
   * @param database a database to provide snapshots of
   */
  public NodeFake(MemoryDb database) {
    this(database, new byte[0]);
  }

  /**
   * Creates a new node fake with the given database.
   *
   * @param database a database to provide snapshots of
   * @param publicKey a public key of the node
   */
  public NodeFake(MemoryDb database, byte[] publicKey) {
    this.database = checkNotNull(database);
    this.publicKey = publicKey.clone();
  }

  /**
   * A no-op.
   *
   * @param transaction a transaction to send
   * @throws NullPointerException if the transaction is null
   */
  @Override
  public HashCode submitTransaction(RawTransaction transaction) {
    checkNotNull(transaction);
    return HashCode.fromBytes(new byte[DEFAULT_HASH_SIZE_BYTES]);
  }

  @Override
  public <ResultT> ResultT withSnapshot(Function<Snapshot, ResultT> snapshotFunction) {
    try (Cleaner cleaner = new Cleaner("NodeFake#withSnapshot")) {
      Snapshot snapshot = database.createSnapshot(cleaner);
      return snapshotFunction.apply(snapshot);
    } catch (CloseFailuresException e) {
      logger.error(e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] getPublicKey() {
    return publicKey.clone();
  }

  /**
   * Returns the underlying database.
   */
  public MemoryDb getDatabase() {
    return database;
  }
}

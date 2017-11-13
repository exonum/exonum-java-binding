package com.exonum.binding.service;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.messages.InternalServerError;
import com.exonum.binding.messages.InvalidTransactionException;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;

/**
 * An implementation of a Node interface for testing purposes.
 * Use it in tests of your handlers that need some data in the database:
 *
 * <pre><code>
 *   MemoryDb db = new MemoryDb();
 *
 *   // Setup database to include some test data
 *   try (Fork fork = db.createFork();
 *        MapIndex balance = new MapIndex("balance", fork)) {
 *     balance.put("John Doe", "$1000.00");
 *     db.merge(fork);
 *   }
 *
 *   // Create a node fake from the database
 *   NodeFake node = new NodeFake(db);
 *
 *   WalletController controller = new WalletController(node);
 *
 *   assertThat(controller.getBalance("John Doe"), equalTo("$1000.00"));
 *
 *   //…
 *
 *   db.close();
 * </code></pre>
 */
public class NodeFake implements Node {

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
  public void submitTransaction(Transaction transaction)
      throws InvalidTransactionException, InternalServerError {
    checkNotNull(transaction);
  }

  @Override
  public Snapshot createSnapshot() {
    return database.createSnapshot();
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

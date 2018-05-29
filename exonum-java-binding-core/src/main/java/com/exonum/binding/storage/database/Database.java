package com.exonum.binding.storage.database;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseableNativeProxy;

/**
 * Represents an underlying Exonum Storage database.
 */
public interface Database extends CloseableNativeProxy {

  /**
   * Creates a new snapshot of the database state.
   *
   * @param cleaner a cleaner to register the snapshot
   * @return a new snapshot of the database state
   */
  Snapshot createSnapshot(Cleaner cleaner);

  /**
   * Creates a new database fork.
   *
   * <p>A fork allows to perform a transaction: a number of independent writes to a database,
   * which then may be <em>atomically</em> applied to the database.
   *
   * @param cleaner a cleaner to register the fork
   * @return a new database fork
   */
  Fork createFork(Cleaner cleaner);
}

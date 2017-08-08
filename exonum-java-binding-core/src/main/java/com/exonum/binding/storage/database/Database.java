package com.exonum.binding.storage.database;

import com.exonum.binding.proxy.AbstractNativeProxy;

/**
 * Represents an underlying Exonum Storage database.
 */
public abstract class Database extends AbstractNativeProxy {

  /**
   * Create a new database proxy.
   *
   * @param nativeHandle a native handle: an implementation-specific reference to a native object.
   * @param owningHandle true if this proxy is responsible to release any native resources;
   */
  Database(long nativeHandle, boolean owningHandle) {
    super(nativeHandle, owningHandle);
  }

  /**
   * Creates a new snapshot of the database state.
   *
   * <p>A caller is responsible to close the snapshot (see {@link View#close()}).
   *
   *  @return a new snapshot of the database state.
   */
  public abstract Snapshot createSnapshot();

  /**
   * Creates a new database fork.
   *
   * <p>A fork allows to perform a transaction: a number of independent writes to a database,
   * which then may be <em>atomically</em> applied to the database.
   *
   * <p>A caller is responsible to close the fork (see {@link View#close()}).
   *
   * @return a new database fork.
   */
  public abstract Fork createFork();
}

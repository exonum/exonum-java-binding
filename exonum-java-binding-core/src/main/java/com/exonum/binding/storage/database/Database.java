package com.exonum.binding.storage.database;

import com.exonum.binding.proxy.AbstractNativeProxy;
import com.exonum.binding.proxy.ProxyContext;

/**
 * Represents an underlying Exonum Storage database.
 */
public abstract class Database extends AbstractNativeProxy {

  /**
   * Create a new database proxy.
   *
   * @param nativeHandle a native handle: an implementation-specific reference to a native object
   * @param owningHandle true if this proxy is responsible to release any native resources
   */
  Database(long nativeHandle, boolean owningHandle) {
    super(nativeHandle, owningHandle);
  }

  /**
   * Creates a new snapshot of the database state.
   *
   * @param context a context to register the snapshot in
   *
   *  @return a new snapshot of the database state
   */
  public abstract Snapshot createSnapshot(ProxyContext context);

  /**
   * Creates a new database fork.
   *
   * <p>A fork allows to perform a transaction: a number of independent writes to a database,
   * which then may be <em>atomically</em> applied to the database.
   *
   * @param context a context to register the fork in
   *
   * @return a new database fork
   */
  public abstract Fork createFork(ProxyContext context);
}

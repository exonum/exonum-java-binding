package com.exonum.binding.service.adapters;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.Snapshot;

/**
 * A factory of views.
 *
 * <p>Enables easier testing of service and transaction adapters: {@link UserServiceAdapter}
 * and {@link UserTransactionAdapter}.
 */
public interface ViewFactory {

  /**
   * Creates a new owning snapshot.
   *
   * @param nativeHandle a handle to the native snapshot object
   * @param cleaner a cleaner to register the destructor
   * @return a new owning snapshot proxy
   */
  Snapshot createSnapshot(long nativeHandle, Cleaner cleaner);

  /**
   * Creates a new owning fork.
   *
   * @param nativeHandle a handle to the native fork object
   * @param cleaner a cleaner to register the destructor
   * @return a new owning fork proxy
   */
  Fork createFork(long nativeHandle, Cleaner cleaner);
}

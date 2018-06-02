package com.exonum.binding.service.adapters;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.Snapshot;

/**
 * A factory of View proxies.
 *
 * <p>This class is thread-safe.
 */
public enum ViewProxyFactory implements ViewFactory {
  INSTANCE;

  /** Returns an instance of this factory. */
  public static ViewFactory getInstance() {
    return INSTANCE;
  }

  @Override
  public Snapshot createSnapshot(long nativeHandle, Cleaner cleaner) {
    return Snapshot.newInstance(nativeHandle, cleaner);
  }

  @Override
  public Fork createFork(long nativeHandle, Cleaner cleaner) {
    return Fork.newInstance(nativeHandle, cleaner);
  }
}

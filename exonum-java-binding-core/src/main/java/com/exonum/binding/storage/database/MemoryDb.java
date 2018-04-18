package com.exonum.binding.storage.database;

import com.exonum.binding.proxy.ProxyContext;
import com.google.common.annotations.VisibleForTesting;

/**
 * An in-memory database for testing purposes. It can create read-only snapshots and read-write
 * forks. The changes made to database forks can be applied to the database state
 * with {@link MemoryDb#merge(Fork)}.
 */
@VisibleForTesting
public class MemoryDb extends Database {

  /**
   * Creates a new empty MemoryDb.
   */
  public MemoryDb() {
    super(nativeCreate(), true);
  }

  @Override
  public Snapshot createSnapshot(ProxyContext context) {
    SnapshotProxy proxy = new SnapshotProxy(nativeCreateSnapshot(getNativeHandle()), this);
    return new Snapshot(proxy, context);
  }

  @Override
  public Fork createFork(ProxyContext context) {
    ForkProxy proxy = new ForkProxy(nativeCreateFork(getNativeHandle()), this);
    return new Fork(proxy, context);
  }

  /**
   * Applies the changes from the given fork to the database state.
   *
   * @param fork a fork to get changes from
   */
  public void merge(Fork fork) {
    ForkProxy proxy = fork.getProxy();
    // This code breaks the law of Demeter :/
    long forkHandle = proxy.getViewNativeHandle();
    nativeMerge(getNativeHandle(), forkHandle);
  }

  @Override
  protected void disposeInternal() {
    nativeFree(getNativeHandle());
  }

  private static native long nativeCreate();

  private native long nativeCreateSnapshot(long dbNativeHandle);

  private native long nativeCreateFork(long dbNativeHandle);

  private native void nativeMerge(long dbNativeHandle, long forkNativeHandle);

  private native void nativeFree(long dbNativeHandle);
}

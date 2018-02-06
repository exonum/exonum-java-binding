package com.exonum.binding.storage.database;

import com.google.common.annotations.VisibleForTesting;

/**
 * An in-memory database for testing purposes. Although it can create
 * both read-only snapshots and read-write forks, the changes made to database forks
 * cannot be applied to the database state, making it effectively stateless.
 *
 * <p>If you need a stateful database for tests, feel free to file a JIRA ticket.
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
  public Snapshot createSnapshot() {
    return new Snapshot(nativeCreateSnapshot(getNativeHandle()), this);
  }

  @Override
  public Fork createFork() {
    return new Fork(nativeCreateFork(getNativeHandle()), this);
  }

  /**
   * Applies the changes from the given fork to the database state.
   *
   * @param fork a fork to get changes from
   */
  public void merge(Fork fork) {
    nativeMerge(getNativeHandle(), fork.getViewNativeHandle());
  }

  @Override
  protected void disposeInternal() {
    nativeFree(getNativeHandle());
  }

  private static native long nativeCreate();

  private native long nativeCreateSnapshot(long nativeDb);

  private native long nativeCreateFork(long nativeDb);

  private native void nativeMerge(long nativeDb, long forkNativeHandle);

  private native void nativeFree(long nativeDb);
}

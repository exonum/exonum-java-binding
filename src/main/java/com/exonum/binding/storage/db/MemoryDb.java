package com.exonum.binding.storage.db;

import com.exonum.binding.storage.connector.Fork;
import com.exonum.binding.storage.connector.Snapshot;

public class MemoryDb implements Database {

  private final long nativeMemoryDb;

  public MemoryDb() {
    this.nativeMemoryDb = nativeCreateMemoryDb();
  }

  @Override
  public void destroyNativeDb() {
    nativeFreeMemoryDb(nativeMemoryDb);
  }

  @Override
  public Snapshot lookupSnapshot() {
    return new Snapshot(nativeLookupSnapshot(this.nativeMemoryDb));
  }

  @Override
  public Fork lookupFork() {
    return new Fork(nativeLookupFork(this.nativeMemoryDb));
  }

  private native long nativeLookupSnapshot(long nativeDb);

  private native long nativeLookupFork(long nativeDb);

  private native long nativeCreateMemoryDb();

  private native void nativeFreeMemoryDb(long nativeDb);
}

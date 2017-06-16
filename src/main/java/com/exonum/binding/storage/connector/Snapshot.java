package com.exonum.binding.storage.connector;

import com.exonum.binding.storage.exception.SnapshotUsageException;

public class Snapshot implements Connect {

  private final long nativeSnapshot;

  public Snapshot(long snapshotObj) {
    this.nativeSnapshot = snapshotObj;
  }

  @Override
  public long getNativeHandle() {
    return nativeSnapshot;
  }

  @Override
  public void close() {
    Views.nativeFreeView(nativeSnapshot);
  }

  @Override
  public void lockWrite() {
    throw new SnapshotUsageException();
  }

  @Override
  public void lockRead() {
    //method do nothing for Snapshot
  }

  @Override
  public void unlockWrite() {
    throw new SnapshotUsageException();
  }

  @Override
  public void unlockRead() {
    //method do nothing for Snapshot
  }
}

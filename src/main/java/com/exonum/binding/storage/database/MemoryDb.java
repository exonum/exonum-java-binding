package com.exonum.binding.storage.database;

public class MemoryDb extends Database {

  public MemoryDb() {
    super(nativeCreate(), true);
  }

  @Override
  public Snapshot createSnapshot() {
    return new Snapshot(nativeCreateSnapshot(getNativeHandle()));
  }

  @Override
  public Fork createFork() {
    return new Fork(nativeCreateFork(getNativeHandle()));
  }

  @Override
  protected void disposeInternal() {
    nativeFree(getNativeHandle());
  }

  private static native long nativeCreate();

  private native long nativeCreateSnapshot(long nativeDb);

  private native long nativeCreateFork(long nativeDb);

  private native void nativeFree(long nativeDb);
}

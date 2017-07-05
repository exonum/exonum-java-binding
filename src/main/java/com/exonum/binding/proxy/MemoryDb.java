package com.exonum.binding.proxy;

public class MemoryDb extends Database {

  public MemoryDb() {
    super(nativeCreate(), true);
  }

  @Override
  public Snapshot createSnapshot() {
    return new Snapshot(nativeCreateSnapshot(nativeHandle));
  }

  @Override
  public Fork createFork() {
    return new Fork(nativeCreateFork(nativeHandle));
  }

  @Override
  void disposeInternal() {
    nativeFree(nativeHandle);
  }

  private static native long nativeCreate();

  private native long nativeCreateSnapshot(long nativeDb);

  private native long nativeCreateFork(long nativeDb);

  private native void nativeFree(long nativeDb);
}

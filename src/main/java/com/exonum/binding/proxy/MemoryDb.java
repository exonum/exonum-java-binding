package com.exonum.binding.proxy;

public class MemoryDb extends Database {

  public MemoryDb() {
    super(nativeCreate(), true);
  }

  @Override
  public Snapshot getSnapshot() {
    return new Snapshot(nativeLookupSnapshot(nativeHandle));
  }

  @Override
  public Fork getFork() {
    return new Fork(nativeLookupFork(nativeHandle));
  }

  @Override
  void disposeInternal() {
    nativeFree(nativeHandle);
  }

  private static native long nativeCreate();

  private native long nativeLookupSnapshot(long nativeDb);

  private native long nativeLookupFork(long nativeDb);

  private native void nativeFree(long nativeDb);
}

package com.exonum.binding.proxy;

public class LevelDb extends Database {

  public LevelDb(String pathToDb) {
    super(nativeCreate(pathToDb), true);
  }

  @Override
  public Snapshot getSnapshot() {
    return new Snapshot(nativeLookupSnapshot(this.nativeHandle));
  }

  @Override
  public Fork getFork() {
    return new Fork(nativeLookupFork(this.nativeHandle));
  }

  @Override
  void disposeInternal() {
    nativeFree(nativeHandle);
  }

  private static native long nativeCreate(String path);

  private native long nativeLookupSnapshot(long nativeHandle);

  private native long nativeLookupFork(long nativeHandle);

  private native void nativeFree(long nativeHandle);
}

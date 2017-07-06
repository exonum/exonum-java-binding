package com.exonum.binding.proxy;

public class LevelDb extends Database {

  public LevelDb(String pathToDb) {
    super(nativeCreate(pathToDb), true);
  }

  @Override
  public Snapshot createSnapshot() {
    return new Snapshot(nativeCreateSnapshot(this.nativeHandle));
  }

  @Override
  public Fork createFork() {
    return new Fork(nativeCreateFork(this.nativeHandle));
  }

  @Override
  void disposeInternal() {
    nativeFree(nativeHandle);
  }

  private static native long nativeCreate(String path);

  private native long nativeCreateSnapshot(long nativeHandle);

  private native long nativeCreateFork(long nativeHandle);

  private native void nativeFree(long nativeHandle);
}

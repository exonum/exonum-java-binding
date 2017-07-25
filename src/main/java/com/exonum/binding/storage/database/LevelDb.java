package com.exonum.binding.storage.database;

public class LevelDb extends Database {

  public LevelDb(String pathToDb) {
    super(nativeCreate(pathToDb), true);
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

  private static native long nativeCreate(String path);

  private native long nativeCreateSnapshot(long nativeHandle);

  private native long nativeCreateFork(long nativeHandle);

  private native void nativeFree(long nativeHandle);
}

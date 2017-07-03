package com.exonum.binding.storage.connector;

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
    Views.nativeFree(nativeSnapshot);
  }
}

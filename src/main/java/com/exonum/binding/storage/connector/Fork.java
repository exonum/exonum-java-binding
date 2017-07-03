package com.exonum.binding.storage.connector;

public class Fork implements Connect {

  private final long nativeFork;

  public Fork(long forkObj) {
    this.nativeFork = forkObj;
  }

  @Override
  public long getNativeHandle() {
    return nativeFork;
  }

  @Override
  public void close() {
    Views.nativeFree(nativeFork);
  }
}

package com.exonum.binding.proxy;

public class Snapshot extends Connect {

  public Snapshot(long nativeHandle) {
    super(nativeHandle, true);
  }

  @Override
  void disposeInternal() {
    Views.nativeFree(nativeHandle);
  }
}

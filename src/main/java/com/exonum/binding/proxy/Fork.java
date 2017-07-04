package com.exonum.binding.proxy;

public class Fork extends Connect {

  public Fork(long nativeHandle) {
    super(nativeHandle, true);
  }

  @Override
  void disposeInternal() {
    Views.nativeFree(nativeHandle);
  }
}

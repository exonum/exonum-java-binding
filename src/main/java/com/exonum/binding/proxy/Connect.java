package com.exonum.binding.proxy;

public abstract class Connect extends AbstractNativeProxy {

  /**
   * @param nativeHandle a native handle: an implementation-specific reference to a native object.
   * @param owningHandle true if this proxy is responsible to release any native resources;
   */
  Connect(long nativeHandle, boolean owningHandle) {
    super(nativeHandle, owningHandle);
  }

  long getNativeHandle() {
    return nativeHandle;
  }
}

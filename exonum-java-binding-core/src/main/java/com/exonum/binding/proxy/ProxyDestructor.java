package com.exonum.binding.proxy;

import java.util.function.LongConsumer;

/**
 * ProxyDestructor is a clean action that destroys a native proxy and closes its native handle.
 *
 * <p>Native proxies do not implement any interface (e.g., NativeProxy) and use this class
 * so that there is no public #close method available in the interface of the proxy,
 * making the risk of misuse smaller.
 *
 * <p>This class is not thread-safe.
 */
public final class ProxyDestructor implements CleanAction {

  private final NativeHandle nativeHandle;
  private final LongConsumer cleanFunction;
  private boolean destroyed;

  /**
   * A destructor of a native proxy. Closes the native handle, so that it can no longer be accessed,
   * and performs a clean action, passing the native handle.
   *
   * <p>If native handle is not valid, does nothing.
   *
   * @param nativeHandle a handle to the native object
   * @param cleanFunction a clean function to perform
   */
  public ProxyDestructor(NativeHandle nativeHandle, LongConsumer cleanFunction) {
    this.nativeHandle = nativeHandle;
    this.cleanFunction = cleanFunction;
    destroyed = false;
  }

  @Override
  public void clean() {
    if (destroyed) {
      return;
    }

    destroyed = true;

    // Do not perform the destructor if the native handle is not valid.
    if (!nativeHandle.isValid()) {
      return;
    }

    long handle = nativeHandle.get();

    // Close the native handle.
    nativeHandle.close();

    // Perform the clean action.
    cleanFunction.accept(handle);
  }
}

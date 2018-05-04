package com.exonum.binding.proxy;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
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
   * Creates a new destructor of a native proxy, registered in the given cleaner.
   *
   * @param cleaner a cleaner to register the destructor in
   * @param nativeHandle a handle to the native object
   * @param destructorFunction a clean function to perform
   */
  @CanIgnoreReturnValue
  public static ProxyDestructor newRegistered(Cleaner cleaner,
                                              NativeHandle nativeHandle,
                                              LongConsumer destructorFunction) {
    checkNotNull(cleaner);
    checkNotNull(nativeHandle);
    checkNotNull(destructorFunction);

    ProxyDestructor d = new ProxyDestructor(nativeHandle, destructorFunction);
    cleaner.add(d);
    return d;
  }

  /**
   * Creates a new destructor of a native proxy.
   *
   * @param nativeHandle a handle to the native object
   * @param destructorFunction a clean function to perform
   */
  public ProxyDestructor(NativeHandle nativeHandle, LongConsumer destructorFunction) {
    this.nativeHandle = nativeHandle;
    this.cleanFunction = destructorFunction;
    destroyed = false;
  }

  /**
   * Closes the native handle, so that it can no longer be accessed, and performs a clean action,
   * passing the native handle value.
   *
   *<p>If native handle is not valid, does nothing.
   *
   * <p>This method is idempotent.
   */
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

package com.exonum.binding.proxy;

/**
 * A temporary base class of a native proxy.
 */
public abstract class AbstractNativeProxy2 {

  /** A handle to the native object. */
  protected final NativeHandle nativeHandle;

  protected AbstractNativeProxy2(NativeHandle nativeHandle) {
    this.nativeHandle = nativeHandle;
  }

  /**
   * Returns a handle to the native object. Equivalent to {@code nativeHandle.get()}.
   *
   * <p>The returned value shall only be passed as an argument to native methods.
   *
   * <p><strong>Warning:</strong> do not cache the return value, as you won't be able
   * to catch use-after-free.
   *
   * @throws IllegalStateException if the native handle is invalid (closed or nullptr)
   */
  protected final long getNativeHandle() {
    return nativeHandle.get();
  }
}

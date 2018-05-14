package com.exonum.binding.proxy;

/**
 * A base class of a native proxy.
 */
public abstract class AbstractNativeProxy {

  /** A handle to the native object. */
  protected final NativeHandle nativeHandle;

  protected AbstractNativeProxy(NativeHandle nativeHandle) {
    this.nativeHandle = nativeHandle;
  }

  /**
   * Returns a handle to the native object if it may be safely used to access the native object.
   * Equivalent to {@code nativeHandle.get()}.
   *
   * <p>The returned value shall only be passed as an argument to native methods.
   *
   * <p><strong>Warning:</strong> do not cache the return value, as you won't be able
   * to catch use-after-free.
   *
   * @throws IllegalStateException if the native handle is invalid (closed or nullptr)
   */
  protected long getNativeHandle() {
    return nativeHandle.get();
  }

  /**
   * Returns true if this proxy has a valid native handle.
   */
  protected final boolean isValidHandle() {
    return nativeHandle.isValid();
  }
}

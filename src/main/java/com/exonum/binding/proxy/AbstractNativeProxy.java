package com.exonum.binding.proxy;

/**
 * A proxy of a native object.
 *
 * <p>You must close a native proxy when it is no longer needed
 * to release any native resources (e.g., destroy a native object).
 * You may use a <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">try-with-resources</a>
 * statement to do that in orderly fashion.
 */
abstract class AbstractNativeProxy implements AutoCloseable {

  /**
   * A reserved value for an invalid native handle, equal to <code>nullptr</code> in C++.
   */
  static final long INVALID_NATIVE_HANDLE = 0L;

  /**
   * A native implementation-specific handle.
   *
   * <p>This value shall only be passed as an argument to native methods.
   */
  final long nativeHandle;

  /**
   * Whether this proxy owns the corresponding native object and is responsible to clean it up.
   */
  private final boolean owningHandle;

  /**
   * Whether the proxy is valid and may be used to access the native object.
   */
  private boolean valid;

  /**
   * @param nativeHandle a native handle: an implementation-specific reference to a native object.
   * @param owningHandle true if this proxy is responsible to release any native resources;
   *                     false — otherwise.
   */
  AbstractNativeProxy(long nativeHandle, boolean owningHandle) {
    this.nativeHandle = nativeHandle;
    this.owningHandle = owningHandle;
    valid = (nativeHandle != INVALID_NATIVE_HANDLE);
  }

  /**
   * Close the native proxy.
   *
   * <p>Notifies the native code that the native object is no longer needed, and may be safely
   * destroyed. Once closed, the proxy becomes invalid.
   *
   * <p>On consecutive invocations does nothing.
   */
  @Override
  public final void close() {
    if (owningHandle && valid) {
      try {
        disposeInternal();
      } finally {
        valid = false;
      }
    }
  }

  /**
   * Destroys the corresponding native object.
   *
   * <p>This method is only called from {@link #close()} and shall not be called directly.
   */
  abstract void disposeInternal();

  /** Returns true if the proxy is valid and may be used to access the native object. */
  final boolean isValid() {
    return valid;
  }
}

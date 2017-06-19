package com.exonum.binding;

/**
 * A handle to a native object.
 */
public interface NativeHandle extends AutoCloseable {
  /**
   * Returns a native implementation-specific handle.
   *
   * <p>Callers should only use this value to pass as an argument to other native methods.
   */
  long getNativeHandle();

  /**
   * Close the native handle.
   *
   * <p>Notifies the native code that this handle is no longer needed, and may be safely
   * destroyed.
   */
  @Override
  void close();
}

package com.exonum.binding.proxy;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;

/**
 * An implementation-specific handle to the native object. Once closed, can no longer be accessed.
 */
public final class NativeHandle implements AutoCloseable {

  /**
   * A reserved value for an invalid native handle, equal to <code>nullptr</code> in C++.
   */
  @VisibleForTesting
  static final long INVALID_NATIVE_HANDLE = 0L;

  private long nativeHandle;

  public NativeHandle(long nativeHandle) {
    this.nativeHandle = nativeHandle;
  }

  /**
   * Returns a native implementation-specific handle if it may be safely used
   * to access the native object.
   *
   * <p>The returned value shall only be passed as an argument to native methods.
   *
   * <p>Warning: do not cache the return value, as you won't be able to catch use-after-free.
   *
   * @throws IllegalStateException if this native handle is invalid (closed or nullptr)
   */
  public long get() {
    checkValid();
    return nativeHandle;
  }

  @Override
  public void close() {
    if (isValid()) {
      invalidate();
    }
  }

  private void checkValid() {
    checkState(isValid(), "This handle is not valid: %s", this);
  }

  /**
   * Returns true if this native handle is valid.
   */
  final boolean isValid() {
    return nativeHandle != INVALID_NATIVE_HANDLE;
  }

  private void invalidate() {
    nativeHandle = INVALID_NATIVE_HANDLE;
  }

}

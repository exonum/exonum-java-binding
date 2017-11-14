package com.exonum.binding.proxy;

import static com.exonum.binding.proxy.ProxyPreconditions.checkValid;

/**
 * A proxy of a native object.
 *
 * <p>A native proxy references the corresponding native object by its
 * implementation-specific handle. If handle is zero, the proxy is considered invalid
 * and will not permit referencing any native object. It is perfectly possible to get
 * an invalid proxy (e.g., if a native method fails to allocate a native object).
 *
 * <p>You must close a native proxy when it is no longer needed
 * to release any resources it holds (e.g., destroy a native object).
 * You may use a <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">try-with-resources</a>
 * statement to do that in orderly fashion.
 * When a proxy is closed, it becomes invalid.
 */
public abstract class AbstractNativeProxy implements NativeProxy {

  /**
   * A reserved value for an invalid native handle, equal to <code>nullptr</code> in C++.
   */
  static final long INVALID_NATIVE_HANDLE = 0L;

  /**
   * Whether this proxy shall dispose any resources when closed
   * (e.g., if it owns the corresponding native object and is responsible to clean it up).
   */
  private final boolean dispose;

  private long nativeHandle;

  /**
   * Creates a native proxy.
   *
   * @param nativeHandle an implementation-specific reference to a native object
   * @param dispose true if this proxy is responsible to release any resources
   *                by calling {@link #disposeInternal}; false — otherwise
   */
  protected AbstractNativeProxy(long nativeHandle, boolean dispose) {
    this.nativeHandle = nativeHandle;
    this.dispose = dispose;
  }

  /**
   * Returns a native implementation-specific handle.
   *
   * <p>The returned value shall only be passed as an argument to native methods.
   *
   * <p>Warning: do not cache the return value, as you won't be able to catch use-after-free.
   *
   * @throws IllegalStateException if this native proxy is invalid (closed or nullptr).
   */
  protected final long getNativeHandle() {
    checkValid(this);
    return getNativeHandleUnsafe();
  }

  /**
   * Returns a native handle.
   *
   * <p>If clients ever need to just get a value (e.g., in their {@link #toString()}
   * implementation), make this package-local.
   */
  private long getNativeHandleUnsafe() {
    return nativeHandle;
  }

  @Override
  public final void close() {
    if (isValid()) {
      try {
        if (dispose) {
          disposeInternal();
        }
      } finally {
        invalidate();
      }
    }
  }

  /** Returns true if the proxy is valid and may be used to access the native object. */
  final boolean isValid() {
    return nativeHandle != INVALID_NATIVE_HANDLE;
  }

  /**
   * Releases any resources owned by this proxy (e.g., the corresponding native object).
   *
   * <p>This method is only called once from {@link #close()} for a <strong>valid</strong>
   * proxy and shall not be called directly.
   */
  protected abstract void disposeInternal();

  private void invalidate() {
    nativeHandle = INVALID_NATIVE_HANDLE;
  }
}

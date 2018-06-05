package com.exonum.binding.proxy;

/**
 * A proxy of a native object that must be explicitly closed.
 *
 * <p>You must close a native proxy when it is no longer needed
 * to release any native resources (e.g., destroy a native object).
 * You may use a <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">try-with-resources</a>
 * statement to do that in orderly fashion.
 * When a proxy is closed, it becomes invalid.
 */
public interface CloseableNativeProxy extends AutoCloseable {

  /**
   * Closes the native proxy and releases any native resources associated with this proxy.
   *
   * <p>Notifies the native code that the native object is no longer needed, and may be safely
   * destroyed. Once closed, the proxy becomes invalid.
   *
   * <p>The implementations must be idempotent — do nothing on consecutive invocations.
   */
  @Override
  void close();
}

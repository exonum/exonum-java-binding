package com.exonum.binding.proxy;

/**
 * Indicates that there were failures when it was attempted to close some native proxies.
 *
 * @see ProxyContext#close()
 */
public final class CloseFailuresException extends Exception {

  /**
   * Constructs a new exception with the specified detail message.  The
   * cause is not initialized, and may subsequently be initialized by
   * a call to {@link #initCause}.
   *
   * @param message the detail message. The detail message is saved for
   *                later retrieval by the {@link #getMessage()} method.
   */
  CloseFailuresException(String message) {
    super(message);
  }
}

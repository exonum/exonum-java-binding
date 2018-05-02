package com.exonum.binding.proxy;

/**
 * A clean action is an operation that is performed to release some resources.
 */
// todo: May add extra operations (proxy-specific, if you need, e.g., proxy type, so that
// the Cleaner may display proper statistics).
public interface CleanAction {

  /**
   * A clean operation to perform. It is recommended that this operation is idempotent.
   */
  void clean();
}

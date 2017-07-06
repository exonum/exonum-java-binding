package com.exonum.binding.proxy;

import com.exonum.binding.annotations.ImproveDocs;

/**
 * Represents a logical connection to (or a view of) a database.
 *
 * <p>There are two sub-types:
 * <ul>
 *   <li>A snapshot, which is a read-only view.</li>
 *   <li>A fork, which is a read-write view.</li>
 * </ul>
 *
 * <p>As any native proxy, a connection must be closed.
 */
@ImproveDocs(
    assignee = "dt",
    reason = "if all connections become non-managed, consider changing the last paragraph."
)
public abstract class Connect extends AbstractNativeProxy {

  /**
   * @param nativeHandle a native handle: an implementation-specific reference to a native object.
   * @param owningHandle true if this proxy is responsible to release any native resources;
   */
  Connect(long nativeHandle, boolean owningHandle) {
    super(nativeHandle, owningHandle);
  }

  long getNativeHandle() {
    return nativeHandle;
  }
}

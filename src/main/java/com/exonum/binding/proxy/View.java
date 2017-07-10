package com.exonum.binding.proxy;

import com.exonum.binding.annotations.ImproveDocs;

/**
 * Represents a view of a database.
 *
 * <p>There are two sub-types:
 * <ul>
 *   <li>A snapshot, which is a <em>read-only</em> view.</li>
 *   <li>A fork, which is a <em>read-write</em> view.</li>
 * </ul>
 *
 * <p>As any native proxy, a view must be closed.
 *
 * @see Snapshot
 * @see Fork
 */
@ImproveDocs(
    assignee = "dt",
    reason = "if all views become non-managed, consider changing the last paragraph."
)
public abstract class View extends AbstractNativeProxy {

  /**
   * @param nativeHandle a native handle: an implementation-specific reference to a native object.
   * @param owningHandle true if this proxy is responsible to release any native resources;
   */
  View(long nativeHandle, boolean owningHandle) {
    super(nativeHandle, owningHandle);
  }
}

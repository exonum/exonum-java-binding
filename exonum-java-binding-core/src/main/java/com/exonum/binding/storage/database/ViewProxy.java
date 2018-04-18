package com.exonum.binding.storage.database;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import com.exonum.binding.annotations.ImproveDocs;
import com.exonum.binding.proxy.AbstractNativeProxy;
import javax.annotation.Nullable;

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
 * @see SnapshotProxy
 * @see ForkProxy
 */
@ImproveDocs(
    assignee = "dt",
    reason = "if all views become non-managed, consider changing the last paragraph."
)
public abstract class ViewProxy extends AbstractNativeProxy {

  /**
   * Create a new view proxy.
   *
   * @param nativeHandle a native handle: an implementation-specific reference to a native object
   * @param dispose true if this proxy is responsible to release any resources
   */
  ViewProxy(long nativeHandle, boolean dispose) {
    super(nativeHandle, dispose);
  }

  ViewProxy(long nativeHandle, boolean dispose, @Nullable MemoryDb db) {
    super(nativeHandle, dispose, (db == null) ? emptySet() : singleton(db));
  }

  /**
   *  Returns a native handle of this view.
   *
   *  @throws IllegalStateException if the view is invalid (closed or nullptr)
   */
  public long getViewNativeHandle() {
    return super.getNativeHandle();
  }
}

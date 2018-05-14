package com.exonum.binding.storage.database;

import com.exonum.binding.proxy.AbstractNativeProxy;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.NativeHandle;

/**
 * Represents a view of a database.
 *
 * <p>There are two sub-types:
 * <ul>
 *   <li>A snapshot, which is a <em>read-only</em> view.</li>
 *   <li>A fork, which is a <em>read-write</em> view.</li>
 * </ul>
 *
 * @see Snapshot
 * @see Fork
 */
public abstract class View extends AbstractNativeProxy {

  private final Cleaner cleaner;

  /**
   * Create a new view proxy.
   *
   * @param nativeHandle a native handle: an implementation-specific reference to a native object
   * @param cleaner a cleaner of resources
   */
  View(NativeHandle nativeHandle, Cleaner cleaner) {
    super(nativeHandle);
    this.cleaner = cleaner;
  }

  /**
   *  Returns a native handle of this view.
   *
   *  @throws IllegalStateException if the view is invalid (closed or nullptr)
   */
  public long getViewNativeHandle() {
    return super.getNativeHandle();
  }

  /**
   * Returns the cleaner of this view.
   */
  public Cleaner getCleaner() {
    return cleaner;
  }
}

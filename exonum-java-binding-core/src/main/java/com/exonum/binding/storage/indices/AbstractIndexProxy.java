package com.exonum.binding.storage.indices;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.proxy.AbstractNativeProxy;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.database.ViewModificationCounter;

/**
 * An abstract super class for proxies of all indices.
 *
 * <p>Each index is created with a database view, either an immutable Snapshot or a read-write Fork.
 * An index has a modification counter to detect when it or the corresponding view is modified.
 */
abstract class AbstractIndexProxy extends AbstractNativeProxy {

  // TODO: consider moving 'dbView' to a super class as 'parents'
  //       (= objects that must not be deleted before this)
  final View dbView;

  /**
   * Needed to detect modifications of this index during iteration over this (or other) indices.
   */
  final ViewModificationCounter modCounter;

  /**
   * Creates a new index.
   *
   * <p>Subclasses shall create a native object and pass a native handle to this constructor.
   *
   * @param nativeHandle a native handle of the created index
   * @param view a database view from which the index has been created
   * @throws NullPointerException if view is null
   */
  AbstractIndexProxy(long nativeHandle, View view) {
    super(nativeHandle, true);
    this.dbView = checkNotNull(view);
    this.modCounter = ViewModificationCounter.getInstance();
  }

  /**
   * Checks that this index <em>can</em> be modified and changes the modification counter.
   *
   * @throws UnsupportedOperationException if the database view is read-only
   */
  void notifyModified() {
    modCounter.notifyModified(castViewToFork());
  }

  /**
   * Checks that a database view is an instance of {@link Fork} — a modifiable database view.
   *
   * @return a modifiable view: a Fork.
   * @throws UnsupportedOperationException if view is read-only or null.
   */
  private Fork castViewToFork() {
    if (!(dbView instanceof Fork)) {
      throw new UnsupportedOperationException("Cannot modify the view: " + dbView
          + "\nUse a Fork to modify any collection.");
    }
    return (Fork) dbView;
  }
}

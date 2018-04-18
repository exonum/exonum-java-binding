package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.StoragePreconditions.checkIndexName;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.proxy.AbstractNativeProxy;
import com.exonum.binding.storage.database.ForkProxy;
import com.exonum.binding.storage.database.ViewModificationCounter;
import com.exonum.binding.storage.database.ViewProxy;

/**
 * An abstract super class for proxies of all indices.
 *
 * <p>Each index is created with a database view, either an immutable SnapshotProxy or a read-write ForkProxy.
 * An index has a modification counter to detect when it or the corresponding view is modified.
 */
abstract class AbstractIndexProxy extends AbstractNativeProxy implements StorageIndex {

  final ViewProxy dbView;

  /**
   * Needed to detect modifications of this index during iteration over this (or other) indices.
   */
  final ViewModificationCounter modCounter;

  private final String name;

  /**
   * Creates a new index.
   *
   * <p>Subclasses shall create a native object and pass a native handle to this constructor.
   *
   * @param nativeHandle a native handle of the created index
   * @param name a name of this index
   * @param view a database view from which the index has been created
   * @throws NullPointerException if any parameter is null
   */
  AbstractIndexProxy(long nativeHandle, String name, ViewProxy view) {
    super(nativeHandle, true, view);
    this.name = checkIndexName(name);
    this.dbView = checkNotNull(view);
    this.modCounter = ViewModificationCounter.getInstance();
  }

  /** Returns the name of this index. */
  @Override
  public final String getName() {
    return name;
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
   * Checks that a database view is an instance of {@link ForkProxy} — a modifiable database view.
   *
   * @return a modifiable view: a ForkProxy.
   * @throws UnsupportedOperationException if view is read-only or null.
   */
  private ForkProxy castViewToFork() {
    if (!(dbView instanceof ForkProxy)) {
      throw new UnsupportedOperationException("Cannot modify the view: " + dbView
          + "\nUse a ForkProxy to modify any collection.");
    }
    return (ForkProxy) dbView;
  }

  @Override
  public String toString() {
    // test_map: ProofMap
    return name + ": " + getClass().getName();
  }
}

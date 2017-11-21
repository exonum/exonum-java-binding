package com.exonum.binding.storage.database;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;

/**
 * A fork is a database view, allowing both read and write operations.
 *
 * <p>A fork allows to perform a transaction: a number of independent writes to a database,
 * which then may be <em>atomically</em> applied to the database state.
 */
public class Fork extends View {

  /**
   * Whether this fork proxy owns the corresponding native object.
   */
  private final boolean owningHandle;

  /**
   * Create a new owning Fork.
   *
   * @param nativeHandle a handle of the native Fork object
   */
  public Fork(long nativeHandle) {
    this(nativeHandle, true);
  }

  /**
   * Creates a new Fork proxy.
   *
   * @param nativeHandle a handle of the native Fork object
   * @param owningHandle whether a proxy owns the corresponding native object and is responsible
   *                     to clean it up
   */
  public Fork(long nativeHandle, boolean owningHandle) {
    this(nativeHandle, owningHandle, null);
  }

  /**
   * Create a new owning Fork.
   *
   * @param nativeHandle a handle of the native Fork object
   * @param memoryDb a database that created the object
   * @throws NullPointerException if memoryDb is null
   */
  Fork(long nativeHandle, MemoryDb memoryDb) {
    this(nativeHandle, true, checkNotNull(memoryDb));
  }

  private Fork(long nativeHandle, boolean owningHandle, @Nullable MemoryDb memoryDb) {
    // A fork proxy shall always call #disposeInternal when closed, so that it is removed
    // from the view modification counter.
    super(nativeHandle, true, memoryDb);
    this.owningHandle = owningHandle;
  }

  @Override
  protected void disposeInternal() {
    ViewModificationCounter.getInstance().remove(this);
    if (owningHandle) {
      Views.nativeFree(getNativeHandle());
    }
  }
}

package com.exonum.binding.storage.database;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A snapshot is a read-only, immutable database view.
 *
 * <p>A snapshot represents database state at the time it was created. Immutability implies that:
 * <ul>
 *   <li>Write operations are prohibited; an attempt to perform a modifying operation
 *       will result in an {@link UnsupportedOperationException}</li>
 *   <li>Database state will not change whilst a snapshot is alive.
 *       As a snapshot requires a growing amount of memory resources as services write
 *       to the database, do <strong>not</strong> keep Snapshots alive for extended periods of time.
 *       Close them as soon as you are finished with them.</li>
 * </ul>
 *
 * @see ForkProxy
 */
public class SnapshotProxy extends ViewProxy {

  /**
   * Creates a new owning SnapshotProxy proxy.
   *
   * @param nativeHandle a handle of the native SnapshotProxy object
   */
  // todo: consider making package-private so that clients aren't able to reference an invalid
  // memory region (or use the knowledge of a registry of native allocations
  // to safely discard such attempts).
  public SnapshotProxy(long nativeHandle) {
    this(nativeHandle, true);
  }

  /**
   * Creates a new SnapshotProxy proxy.
   *
   * @param nativeHandle a handle of the native SnapshotProxy object
   * @param owningHandle whether a proxy owns the corresponding native object and is responsible
   *                     to clean it up
   */
  public SnapshotProxy(long nativeHandle, boolean owningHandle) {
    super(nativeHandle, owningHandle);
  }

  /**
   * Creates a new owning SnapshotProxy proxy.
   *
   * @param nativeHandle a handle of the native SnapshotProxy object
   * @param memoryDb a database that created this proxy
   * @throws NullPointerException if database is null
   */
  SnapshotProxy(long nativeHandle, MemoryDb memoryDb) {
    super(nativeHandle, true, checkNotNull(memoryDb));
  }

  @Override
  protected void disposeInternal() {
    Views.nativeFree(getNativeHandle());
  }
}

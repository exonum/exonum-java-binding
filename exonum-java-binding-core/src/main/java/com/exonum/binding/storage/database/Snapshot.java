package com.exonum.binding.storage.database;

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
 * @see Fork
 */
public class Snapshot extends View {

  public Snapshot(long nativeHandle) {
    super(nativeHandle, true);
  }

  @Override
  protected void disposeInternal() {
    Views.nativeFree(getNativeHandle());
  }
}

package com.exonum.binding.proxy;

import com.exonum.binding.annotations.ImproveDocs;

/**
 * A snapshot is a read-only, immutable database view.
 *
 * <p>A snapshot represents database state at the time it was created. Immutability implies that:
 * <ul>
 *   <li>Write operations are prohibited; an attempt to perform a modifying operation
 *       will result in a RuntimeException</li>
 *   <li>Database state will not change whilst a snapshot is alive.
 *       As a snapshot requires a growing amount of memory resources as services write
 *       to the database, do <strong>not</strong> keep Snapshots alive for extended periods of time.
 *       Close them as soon as you are finished with them.</li>
 * </ul>
 *
 * @see Fork
 */
@ImproveDocs(
    assignee = "dt",
    reason = "Specify a particular instance of RuntimeException, thrown by collections."
)
public class Snapshot extends Connect {

  public Snapshot(long nativeHandle) {
    super(nativeHandle, true);
  }

  @Override
  void disposeInternal() {
    Views.nativeFree(nativeHandle);
  }
}

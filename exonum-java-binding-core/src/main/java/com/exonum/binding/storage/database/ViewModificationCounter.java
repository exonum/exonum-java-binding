package com.exonum.binding.storage.database;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * A listener of fork modification events.
 *
 * <p>Forks are added lazily when they are modified.
 *
 * <p>The class is thread-safe if {@link ViewProxy}s <strong>are not shared</strong> among threads
 * (i.e., if each thread has its own Views, which must be the case for Views are not thread-safe).
 * Such property is useful in the integration tests.
 */
// TODO(dt): when we migrate to RocksDB, extract the interface and implement it inside
//           each collection.
// TODO(dt): when/if we implement auto-resource management, review this class as it keeps
//           strong references to Forks
public class ViewModificationCounter {

  static final int INITIAL_COUNT = 0;

  private static final ViewModificationCounter instance = new ViewModificationCounter();

  private final ConcurrentMap<ForkProxy, Integer> modificationCounters;

  ViewModificationCounter() {
    modificationCounters = new ConcurrentHashMap<>();
  }

  public static ViewModificationCounter getInstance() {
    return instance;
  }

  /**
   * Remove the fork from the listener.
   */
  void remove(ForkProxy fork) {
    modificationCounters.remove(fork);
  }

  /**
   * Notifies that the fork is modified.
   *
   * <p>Each invocation increases the modification counter of the fork. Initial value is zero.
   * @param fork a modified (or about to be modified) fork.
   * @throws NullPointerException if fork is null.
   */
  public void notifyModified(ForkProxy fork) {
    Integer nextCount = getModificationCount(fork) + 1;
    modificationCounters.put(fork, nextCount);
  }

  /**
   * Returns true if the view has been modified since the given modCount.
   */
  public boolean isModifiedSince(ViewProxy view, Integer modCount) {
    if (view instanceof SnapshotProxy) {
      return false;
    }
    Integer currentModCount = getModificationCount(view);
    return !modCount.equals(currentModCount);
  }

  /**
   * Returns the current value of the modification counter of the given view.
   *
   * @return zero for {@link SnapshotProxy}s, the current value of the modification counter
   *         for a {@link ForkProxy} (may be negative).
   */
  public Integer getModificationCount(ViewProxy view) {
    if (view instanceof SnapshotProxy) {
      return 0;
    }
    return modificationCounters.getOrDefault(view, INITIAL_COUNT);
  }
}

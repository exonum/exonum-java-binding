package com.exonum.binding.storage.database;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.proxy.ProxyDestructor;

/**
 * A snapshot is a read-only, immutable database view.
 *
 * <p>A snapshot represents database state at the time it was created. Immutability implies that:
 * <ul>
 *   <li>Write operations are prohibited; an attempt to perform a modifying operation
 *       will result in an {@link UnsupportedOperationException}</li>
 *   <li>Database state will not change whilst a snapshot is alive.
 * </ul>
 *
 * @see Fork
 */
public final class Snapshot extends View {

  /**
   * Creates a new owning Snapshot proxy.
   *
   * @param nativeHandle a handle of the native Snapshot object
   */
  // todo: consider making package-private so that clients aren't able to reference an invalid
  // memory region (currently the registry of native allocations is required to be enabled
  // to safely discard such attempts).
  public static Snapshot newInstance(long nativeHandle, Cleaner cleaner) {
    return newInstance(nativeHandle, true, cleaner);
  }

  /**
   * Creates a new Snapshot proxy.
   * @param nativeHandle a handle of the native Snapshot object
   * @param owningHandle whether a proxy owns the corresponding native object and is responsible
   *                     to clean it up
   * @param cleaner a cleaner to destroy the native object
   */
  public static Snapshot newInstance(long nativeHandle, boolean owningHandle, Cleaner cleaner) {
    checkNotNull(cleaner, "cleaner");

    NativeHandle h = new NativeHandle(nativeHandle);
    ProxyDestructor.newRegistered(cleaner, h, Snapshot.class, nh -> {
      if (owningHandle) {
        Views.nativeFree(nh);
      }
    });

    return new Snapshot(h, cleaner);
  }

  private Snapshot(NativeHandle nativeHandle, Cleaner cleaner) {
    super(nativeHandle, cleaner);
  }
}

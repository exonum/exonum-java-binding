package com.exonum.binding.storage.database;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.proxy.ProxyDestructor;

/**
 * A fork is a database view, allowing both read and write operations.
 *
 * <p>A fork allows to perform a transaction: a number of independent writes to a database,
 * which then may be <em>atomically</em> applied to the database state.
 */
public class Fork extends View {

  /**
   * Creates a new owning Fork proxy.
   *
   * @param nativeHandle a handle of the native Fork object
   * @param cleaner a cleaner to perform any operations
   */
  public static Fork newInstance(long nativeHandle, Cleaner cleaner) {
    return newInstance(nativeHandle, true, cleaner);
  }

  /**
   * Creates a new Fork proxy.
   *
   * @param nativeHandle a handle of the native Fork object
   * @param owningHandle whether a proxy owns the corresponding native object and is responsible
   *                     to clean it up
   * @param cleaner a cleaner to perform any operations
   */
  public static Fork newInstance(long nativeHandle, boolean owningHandle, Cleaner cleaner) {
    checkNotNull(cleaner, "cleaner");

    NativeHandle h = new NativeHandle(nativeHandle);
    // Add an action destroying the native peer if necessary.
    ProxyDestructor.newRegistered(cleaner, h, nh -> {
      if (owningHandle) {
        Views.nativeFree(nh);
      }
    });

    // Create the fork
    Fork f = new Fork(h, cleaner);

    // Add the action that unregisters the fork separately so that it is always invoked.
    cleaner.add(() -> ViewModificationCounter.getInstance().remove(f));

    return f;
  }

  /**
   * Create a new owning Fork.
   *
   * @param nativeHandle a handle of the native Fork object
   */
  private Fork(NativeHandle nativeHandle, Cleaner cleaner) {
    super(nativeHandle, cleaner);
  }
}

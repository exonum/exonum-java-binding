/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.core.storage.database;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.proxy.ProxyDestructor;

/**
 * A fork is a database view, allowing both read and write operations.
 *
 * <p>A fork represents the database state at the time it was created <em>plus</em> any changes
 * to the database made using this fork.
 *
 * <p>A fork allows to perform
 * a {@linkplain com.exonum.binding.core.transaction.Transaction transaction}: a number
 * of independent writes to the database, which then may be <em>atomically</em> applied
 * (i.e. committed) to the database and change the database state.
 */
public final class Fork extends View {

  /**
   * A destructor of the native fork object. This class keeps a destructor to be able
   * to cancel it on peer ownership transfer, happening in {@link #intoPatch()}.
   */
  private final ProxyDestructor destructor;

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
    ProxyDestructor destructor = ProxyDestructor.newRegistered(cleaner, h, Fork.class, nh -> {
      if (owningHandle) {
        Views.nativeFree(nh);
      }
    });

    // Create a cleaner for collections. A separate cleaner is needed to be able to destroy
    // the objects depending on the fork when it is converted into patch and invalidated
    Cleaner forkCleaner = new Cleaner();
    cleaner.add(forkCleaner::close);

    return new Fork(h, destructor, forkCleaner);
  }

  /**
   * Create a new owning Fork.
   *
   * @param nativeHandle a handle of the native Fork object
   * @param destructor a destructor of the native peer, registered with the parent cleaner
   * @param forkCleaner a cleaner for objects depending on the fork
   */
  private Fork(NativeHandle nativeHandle, ProxyDestructor destructor, Cleaner forkCleaner) {
    super(nativeHandle, forkCleaner, new IncrementalModificationCounter(), true);
    this.destructor = destructor;
  }

  /**
   * Converts this fork into a patch that can be merged into the database.
   * This method will close any resources registered with {@linkplain #getCleaner() its cleaner}
   * (typically, indexes, iterators), convert this fork into a patch, invalidate this fork,
   * and return a handle to the patch. The caller is responsible for properly destroying
   * the returned patch.
   *
   * <p>Subsequent operations with the fork are prohibited.
   *
   * @return a handle to the patch obtained from this fork
   * @see <a href="https://exonum.com/doc/version/0.12/architecture/merkledb/#patches">
   *   MerkleDB Patches</a>
   */
  NativeHandle intoPatch() {
    checkState(nativeCanConvertIntoPatch(getNativeHandle()),
        "This fork cannot be converted into patch");

    // Close all resources depending on this fork
    try {
      getCleaner().close();
    } catch (CloseFailuresException e) {
      // Destroy this fork and abort the operation if there are any failures
      destructor.clean();
      throw new IllegalStateException("intoPatch aborted because some dependent resources "
          + "did not close properly", e);
    }

    try {
      // Cancel the destructor, transferring the ownership of this object to the native code
      // (including the responsibility to destroy it).
      destructor.cancel();

      // Convert into patch
      // TODO(@bogdanov): What if this operation fails? What are the possible failures?
      //   Shall we transfer ownership:
      //   - [current solution] before calling intoPatch, making the native code *always*
      //     responsible for fork clean-up?
      //   - after successful completion (= if it completes exceptionally, we must clean;
      //     if successfully — native code cleans)
      //   - in some successful and exceptional scenarios (= will result in a complex protocol
      //     with custom exceptions, for, I think, no practical reason)?
      long patchNativeHandle = nativeIntoPatch(getNativeHandle());

      return new NativeHandle(patchNativeHandle);
    } finally {
      // Invalidate the native handle to make the fork proxy inaccessible.
      // Done manually as we cancelled the proxy destructor.
      nativeHandle.close();
    }
  }

  /**
   * Returns true if this fork can be converted into patch.
   */
  private static native boolean nativeCanConvertIntoPatch(long nativeHandle);

  /**
   * Converts this fork into patch, consuming the object, and returns the native handle
   * to the patch.
   * TODO(@bogdanov) Document the clean-up guarantees of this method once ^ is clarified.
   */
  private static native long nativeIntoPatch(long nativeHandle);
}

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
 * A fork is a database access object, allowing both read and write operations.
 *
 * <p>A fork represents the database state at the time it was created <em>plus</em> any changes
 * to the database made using this fork.
 *
 * <p>A fork allows to perform a transaction: a number of independent writes to the database,
 * which then may be <em>atomically</em> applied (i.e. committed) to the database and change
 * the database state.
 *
 * <p>The Fork does not modify the index name upon <em>address resolution</em>.
 */
public final class Fork extends AbstractAccess {

  /**
   * A destructor of the native fork object. This class keeps a destructor to be able
   * to cancel it on peer ownership transfer, happening in {@link #intoPatch()}.
   */
  private final ProxyDestructor destructor;
  /**
   * A cleaner for this fork.
   */
  private final Cleaner forkCleaner;
  /**
   * A cleaner for objects depending on the fork. A separate cleaner is needed to be able to destroy
   * the objects depending on the fork, primarily — indexes, when it is converted into patch
   * and invalidated or rolled-back (which requires collection invalidation).
   *
   * <p>It is a "child" of the {@link #forkCleaner} which destroys the fork itself and,
   * through this cleaner, any dependent objects.
   */
  private Cleaner indexCleaner;

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
   * @param cleaner a cleaner to destroy this fork and any dependent objects
   */
  public static Fork newInstance(long nativeHandle, boolean owningHandle, Cleaner cleaner) {
    checkNotNull(cleaner, "cleaner");

    NativeHandle h = new NativeHandle(nativeHandle);
    // Add an action destroying the native peer if necessary.
    ProxyDestructor destructor = ProxyDestructor.newRegistered(cleaner, h, Fork.class, nh -> {
      if (owningHandle) {
        AbstractAccess.nativeFree(nh);
      }
    });

    return new Fork(h, destructor, cleaner);
  }

  /**
   * Create a new owning Fork.
   *
   * @param nativeHandle a handle of the native Fork object
   * @param destructor a destructor of the native peer, registered with the parent cleaner
   * @param parentCleaner a cleaner for this fork
   */
  private Fork(NativeHandle nativeHandle, ProxyDestructor destructor, Cleaner parentCleaner) {
    super(nativeHandle, true);
    this.destructor = destructor;
    this.forkCleaner = parentCleaner;
    replaceIndexCleaner();
  }

  @Override
  public Cleaner getCleaner() {
    return indexCleaner;
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
   * @see <a href="https://exonum.com/doc/version/0.13-rc.2/architecture/merkledb/#patches">
   *   MerkleDB Patches</a>
   */
  NativeHandle intoPatch() {
    checkState(nativeCanConvertIntoPatch(getNativeHandle()),
        "This fork cannot be converted into patch");

    // Close all resources depending on this fork
    try {
      indexCleaner.close();
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

      // Convert into patch. This operation may throw RuntimeException.
      // nativeHandle of the Fork can no longer be used after this operation.
      long patchNativeHandle = nativeIntoPatch(getNativeHandle());

      return new NativeHandle(patchNativeHandle);
    } finally {
      // Invalidate the native handle to make the fork proxy inaccessible.
      // Done manually as we cancelled the proxy destructor.
      nativeHandle.close();
    }
  }

  /**
   * Creates in-memory checkpoint of the current state of this Fork. A checkpoint allows to restore
   * the state of the Fork by reverting the changes made since the last checkpoint operation with
   * {@link #rollback()}. The changes made <em>before</em> the last checkpoint cannot be reverted,
   * because each new checkpoint replaces the previous: checkpoints are not stacked.
   *
   * <p>Creating a checkpoint will invalidate all collections that were instantiated with this fork.
   *
   * <p>This operation is not intended to be used by services.
   */
  public void createCheckpoint() {
    // As stacked (nested) checkpoints are not supported, this operation must not be used by
    // the client code, because in case of an exception it will make the framework
    // unable to revert the changes made by the service before the service created
    // a checkpoint: ECR-3611
    checkState(nativeCanRollback(getNativeHandle()),
        "This fork does not support checkpoints");

    closeDependentObjects();

    nativeCreateCheckpoint(getNativeHandle());
  }

  /**
   * Rollbacks changes to the latest checkpoint. If no checkpoints were created, rollbacks all
   * changes made by this fork. Rollback affects only changes made with this particular
   * Fork instance.
   *
   * <p>Rollback will invalidate all collections that were created with this fork.
   *
   * <p>This operation is not intended to be used by services.
   */
  public void rollback() {
    checkState(nativeCanRollback(getNativeHandle()),
        "This fork does not support rollbacks");

    closeDependentObjects();

    nativeRollback(getNativeHandle());
  }

  private void closeDependentObjects() {
    // Clear the registry of opened indexes as they will be closed
    clearOpenIndexes();

    // Close the active collections (and any other dependent objects),
    // as rollback requires their invalidation
    try {
      indexCleaner.close();
    } catch (CloseFailuresException e) {
      // Close failures must not normally happen and usually indicate a serious framework error,
      // hence we abort the operation. However, it is not always caused by an error
      // in the framework, as the client code can register its own operations in the Cleaner,
      // provided by this fork.
      destructor.clean();
      throw new IllegalStateException(
          "Operation aborted due to some objects that had failed to close", e);
    }

    // Create a new cleaner for indexes instead of the recently closed
    replaceIndexCleaner();
  }

  private void replaceIndexCleaner() {
    // Create a new cleaner for collections
    indexCleaner = new Cleaner();
    // Register in the parent cleaner
    forkCleaner.add(indexCleaner::close);
  }

  /**
   * Returns true if this fork can be converted into patch.
   */
  private static native boolean nativeCanConvertIntoPatch(long nativeHandle);

  /**
   * Converts this fork into patch, consuming the object, and returns the native handle
   * to the patch.
   *
   * <p>In case of failure RuntimeException is thrown and provided nativeHandle is
   * invalidated.
   */
  private static native long nativeIntoPatch(long nativeHandle);

  /**
   * Returns true if creating checkpoints and performing rollbacks is
   * possible with this particular Fork instance.
   *
   * @see #createCheckpoint()
   * @see #rollback()
   */
  private static native boolean nativeCanRollback(long nativeHandle);

  /**
   * Creates in-memory checkpoint that can be used to rollback changes.
   */
  private static native void nativeCreateCheckpoint(long nativeHandle);

  /**
   * Rollback changes to the latest checkpoint. Affects only changes made with
   * this particular Fork instance.
   */
  private static native void nativeRollback(long nativeHandle);
}

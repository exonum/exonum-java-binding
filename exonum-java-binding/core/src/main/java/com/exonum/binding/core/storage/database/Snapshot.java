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

import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.proxy.ProxyDestructor;

/**
 * A snapshot is a read-only, immutable database view.
 *
 * <p>A snapshot represents database state at the time it was created. Immutability implies that:
 * <ul>
 *   <li>Write operations are prohibited; an attempt to perform a modifying operation
 *       will result in an {@link UnsupportedOperationException}.
 *   <li>Database state <em>represented by this snapshot</em> does not change. Newly created
 *   snapshots, however, might correspond to a new database state.
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
  //   memory region (currently the registry of native allocations is required to be enabled
  //   to safely discard such attempts): https://jira.bf.local/browse/ECR-471,
  //   https://jira.bf.local/browse/ECR-1525
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
    super(nativeHandle, cleaner, ImmutableModificationCounter.INSTANCE, false);
  }
}

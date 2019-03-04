/*
 * Copyright 2018 The Exonum Team
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

package com.exonum.binding.storage.database;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.proxy.ProxyDestructor;

/**
 * A fork is a database view, allowing both read and write operations.
 *
 * <p>A fork represents the database state at the time it was created <em>plus</em> any changes
 * to the database made using this fork.
 *
 * <p>A fork allows to perform
 * a {@linkplain com.exonum.binding.transaction.Transaction transaction}: a number of independent
 * writes to the database, which then may be <em>atomically</em> applied (i.e. committed)
 * to the database and change the database state.
 */
public final class Fork extends View {

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
    ProxyDestructor.newRegistered(cleaner, h, Fork.class, nh -> {
      if (owningHandle) {
        Views.nativeFree(nh);
      }
    });

    return new Fork(h, cleaner);
  }

  /**
   * Create a new owning Fork.
   *
   * @param nativeHandle a handle of the native Fork object
   */
  private Fork(NativeHandle nativeHandle, Cleaner cleaner) {
    super(nativeHandle, cleaner, new IncrementalModificationCounter(), true);
  }
}

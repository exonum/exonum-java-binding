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

package com.exonum.binding.core.storage.indices;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.core.proxy.AbstractNativeProxy;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.storage.database.AbstractAccess;
import com.exonum.binding.core.storage.database.Fork;

/**
 * An abstract super class for proxies of all indices.
 *
 * <p>Each index is created with a database access, either an immutable Snapshot or a read-write
 * Fork. An index has a modification counter to detect when it is modified.
 */
abstract class AbstractIndexProxy extends AbstractNativeProxy implements StorageIndex {

  final AbstractAccess dbAccess;

  /** Needed to detect modifications of this index during iteration over this index. */
  final ModificationCounter modCounter;

  private final IndexAddress address;

  /**
   * Creates a new index.
   *
   * <p>Subclasses shall create a native object and pass a native handle to this constructor.
   *
   * @param nativeHandle a native handle of the created index
   * @param address the address of this index
   * @param access a database access from which the index has been created
   * @throws NullPointerException if any parameter is null
   */
  AbstractIndexProxy(NativeHandle nativeHandle, IndexAddress address, AbstractAccess access) {
    super(nativeHandle);
    this.address = checkNotNull(address);
    this.dbAccess = access;
    this.modCounter = ModificationCounter.forAccess(access);
  }

  @Override
  public IndexAddress getAddress() {
    return address;
  }

  /**
   * Checks that this index <em>can</em> be modified and changes the modification counter.
   *
   * @throws UnsupportedOperationException if the database access is read-only
   */
  void notifyModified() {
    checkCanModify();
    modCounter.notifyModified();
  }

  /**
   * Checks that a database access is an instance of {@link Fork} — a modifiable database access.
   *
   * @throws UnsupportedOperationException if access is read-only or null.
   */
  private void checkCanModify() {
    if (!(dbAccess.canModify())) {
      throw new UnsupportedOperationException(
          "Cannot modify the access: " + dbAccess + "\nUse a Fork to modify any collection.");
    }
  }

  @Override
  public String toString() {
    // test_map: ProofMap
    return getName() + ": " + getClass().getName();
  }
}

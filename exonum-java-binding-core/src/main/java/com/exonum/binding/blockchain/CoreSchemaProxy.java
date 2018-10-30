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
 *
 */

package com.exonum.binding.blockchain;

import static com.exonum.binding.common.serialization.StandardSerializers.fixed64;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.proxy.ProxyDestructor;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.ListIndex;
import com.exonum.binding.storage.indices.ListIndexProxy;
import com.exonum.binding.storage.indices.ProofListIndexProxy;

/**
 * A proxy for informational schema maintained by Exonum core.
 * It provides read-only access to all indexes and blockchain information described in the schema.
 */
final class CoreSchemaProxy {

  private final NativeHandle nativeHandle;
  private final View dbView;

  private CoreSchemaProxy(NativeHandle nativeHandle, View dbView) {
    this.nativeHandle = nativeHandle;
    this.dbView = dbView;
  }

  /**
   * Constructs a schema proxy for a given dbView.
   */
  static CoreSchemaProxy newInstance(View dbView) {
    long nativeCreate = nativeCreate(dbView.getViewNativeHandle());
    NativeHandle nativeHandle = new NativeHandle(nativeCreate);

    Cleaner cleaner = dbView.getCleaner();
    ProxyDestructor.newRegistered(cleaner, nativeHandle, CoreSchemaProxy.class,
        CoreSchemaProxy::nativeFree);

    return new CoreSchemaProxy(nativeHandle, dbView);
  }

  /**
   * Returns the height of the latest committed block.
   *
   * @throws RuntimeException if the "genesis block" was not created
   */
  long getHeight() {
    return nativeGetHeight(nativeHandle.get());
  }

  /**
   * Returns an list index containing a block hash for every block height
   * (represented by list index id).
   */
  ListIndex<HashCode> getAllBlockHashes() {
    return ListIndexProxy.newInstance(
        CoreCollection.ALL_BLOCK_HASHES, dbView, StandardSerializers.hash());
  }

  /**
   * Returns an proof list index containing block hashes for the given height.
   */
  ProofListIndexProxy<HashCode> getBlockTransactions(long height) {
    byte[] id = fixed64().toBytes(height);
    return ProofListIndexProxy.newInGroupUnsafe(
        CoreCollection.BLOCK_TRANSACTIONS, id, dbView, StandardSerializers.hash());
  }

  private static native long nativeCreate(long viewNativeHandle);

  private static native void nativeFree(long nativeHandle);

  private static native long nativeGetHeight(long nativeHandle);

  private static native byte[] nativeGetLastBlock(long nativeHandle);

  private static final class CoreCollection {
    private static final String BLOCK_TRANSACTIONS = "block_transactions";
    private static final String ALL_BLOCK_HASHES = "block_hashes_by_height";
  }

}

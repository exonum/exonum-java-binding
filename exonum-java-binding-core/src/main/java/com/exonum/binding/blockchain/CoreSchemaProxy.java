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

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.common.configuration.StoredConfiguration;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.common.serialization.json.StoredConfigurationGsonSerializer;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.proxy.ProxyDestructor;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.ListIndex;
import com.exonum.binding.storage.indices.ListIndexProxy;
import com.exonum.binding.storage.indices.ProofListIndexProxy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A proxy class for the blockchain::Schema struct maintained by Exonum core.
 * Please refer to the
 * <a href="https://docs.rs/exonum/latest/exonum/blockchain/struct.Schema.html">doc</a> for details.
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
    long nativePointer = nativeCreate(dbView.getViewNativeHandle());
    NativeHandle nativeHandle = new NativeHandle(nativePointer);

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
        CoreIndex.ALL_BLOCK_HASHES, dbView, StandardSerializers.hash());
  }

  /**
   * Returns an proof list index containing block hashes for the given height.
   */
  ProofListIndexProxy<HashCode> getBlockTransactions(long height) {
    checkArgument(height >= 0, "Height shouldn't be negative, but was %s", height);
    byte[] id = toCoreStorageKey(height);
    return ProofListIndexProxy.newInGroupUnsafe(
        CoreIndex.BLOCK_TRANSACTIONS, id, dbView, StandardSerializers.hash());
  }

  /**
   * Returns the configuration for the latest height of the blockchain.
   *
   * @throws RuntimeException if the "genesis block" was not created
   */
  StoredConfiguration getActualConfiguration() {
    String rawConfiguration = nativeGetActualConfiguration(nativeHandle.get());

    return StoredConfigurationGsonSerializer.fromJson(rawConfiguration);
  }

  private static native long nativeCreate(long viewNativeHandle);

  private static native void nativeFree(long nativeHandle);

  private static native long nativeGetHeight(long nativeHandle);

  private static native String nativeGetActualConfiguration(long nativeHandle);

  /**
   * Returns the latest committed block.
   *
   * @throws RuntimeException if the "genesis block" was not created
   */
  @SuppressWarnings("unused") //TODO: Will be done in the next task
  private static native byte[] nativeGetLastBlock(long nativeHandle);

  private byte[] toCoreStorageKey(long value) {
    return ByteBuffer.allocate(Long.BYTES)
        .order(ByteOrder.BIG_ENDIAN)
        .putLong(value)
        .array();
  }

  /**
   * Mapping for Exonum core indexes by name.
   */
  private static final class CoreIndex {
    private static final String PREFIX = "core.";
    private static final String BLOCK_TRANSACTIONS = PREFIX + "block_transactions";
    private static final String ALL_BLOCK_HASHES = PREFIX + "block_hashes_by_height";
  }

}

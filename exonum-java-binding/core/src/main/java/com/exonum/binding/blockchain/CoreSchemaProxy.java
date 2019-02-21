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

import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.blockchain.serialization.BlockSerializer;
import com.exonum.binding.blockchain.serialization.TransactionLocationSerializer;
import com.exonum.binding.blockchain.serialization.TransactionResultSerializer;
import com.exonum.binding.common.configuration.StoredConfiguration;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.proxy.ProxyDestructor;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.ListIndex;
import com.exonum.binding.storage.indices.ListIndexProxy;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.MapIndexProxy;
import com.exonum.binding.storage.indices.ProofListIndexProxy;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
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
  private static final Serializer<Block> blockSerializer = BlockSerializer.INSTANCE;
  private static final Serializer<TransactionLocation> transactionLocationSerializer =
      TransactionLocationSerializer.INSTANCE;
  private static final Serializer<TransactionResult> transactionResultSerializer =
      TransactionResultSerializer.INSTANCE;
  private static final Serializer<TransactionMessage> transactionMessageSerializer =
      StandardSerializers.transactionMessage();

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
  ListIndex<HashCode> getBlockHashes() {
    return ListIndexProxy.newInstance(
        CoreIndex.ALL_BLOCK_HASHES, dbView, StandardSerializers.hash());
  }

  /**
   * Returns an proof list index containing block hashes for the given height.
   *
   * @throws IllegalArgumentException if the height is negative or there is no block at given height
   */
  ProofListIndexProxy<HashCode> getBlockTransactions(long blockHeight) {
    checkArgument(blockHeight >= 0, "Height shouldn't be negative, but was %s", blockHeight);
    long blockchainHeight = getHeight();
    checkArgument(
        blockchainHeight >= blockHeight,
        "Height should be less or equal compared to blockchain height %s, but was %s",
        blockchainHeight,
        blockHeight);
    byte[] id = toCoreStorageKey(blockHeight);
    return ProofListIndexProxy.newInGroupUnsafe(
        CoreIndex.BLOCK_TRANSACTIONS, id, dbView, StandardSerializers.hash());
  }

  /**
   * Returns a map that stores a block object for every block hash.
   */
  MapIndex<HashCode, Block> getBlocks() {
    return MapIndexProxy.newInstance(
        CoreIndex.BLOCKS, dbView, StandardSerializers.hash(), blockSerializer);
  }

  /**
   * Returns the latest committed block.
   *
   * @throws RuntimeException if the "genesis block" was not created
   */
  Block getLastBlock() {
    return blockSerializer.fromBytes(nativeGetLastBlock(nativeHandle.get()));
  }

  /**
   * Returns a map of transaction messages identified by their SHA-256 hashes.
   */
  MapIndex<HashCode, TransactionMessage> getTxMessages() {
    return MapIndexProxy.newInstance(CoreIndex.TRANSACTIONS, dbView, StandardSerializers.hash(),
        transactionMessageSerializer);
  }

  /**
   * Returns a map with a key-value pair of a transaction hash and execution result.
   */
  ProofMapIndexProxy<HashCode, TransactionResult> getTxResults() {
    return ProofMapIndexProxy.newInstance(CoreIndex.TRANSACTIONS_RESULTS, dbView,
        StandardSerializers.hash(), transactionResultSerializer);
  }

  /**
   * Returns a map that keeps the block height and transaction position inside the block for every
   * transaction hash.
   */
  MapIndex<HashCode, TransactionLocation> getTxLocations() {
    return MapIndexProxy.newInstance(CoreIndex.TRANSACTIONS_LOCATIONS, dbView,
        StandardSerializers.hash(), transactionLocationSerializer);
  }

  /**
   * Returns the configuration for the latest height of the blockchain.
   *
   * @throws RuntimeException if the "genesis block" was not created
   */
  StoredConfiguration getActualConfiguration() {
    String rawConfiguration = nativeGetActualConfiguration(nativeHandle.get());

    return json().fromJson(rawConfiguration, StoredConfiguration.class);
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
    private static final String TRANSACTIONS = PREFIX + "transactions";
    private static final String BLOCKS = PREFIX + "blocks";
    private static final String TRANSACTIONS_RESULTS = PREFIX + "transaction_results";
    private static final String TRANSACTIONS_LOCATIONS = PREFIX + "transactions_locations";
  }

}

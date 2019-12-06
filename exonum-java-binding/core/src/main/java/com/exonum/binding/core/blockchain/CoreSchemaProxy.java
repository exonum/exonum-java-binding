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

package com.exonum.binding.core.blockchain;

import static com.exonum.binding.common.serialization.StandardSerializers.protobuf;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.blockchain.serialization.BlockSerializer;
import com.exonum.binding.core.blockchain.serialization.TransactionLocationSerializer;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.proxy.ProxyDestructor;
import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.core.storage.indices.EntryIndexProxy;
import com.exonum.binding.core.storage.indices.KeySetIndexProxy;
import com.exonum.binding.core.storage.indices.ListIndex;
import com.exonum.binding.core.storage.indices.ListIndexProxy;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.core.storage.indices.MapIndexProxy;
import com.exonum.binding.core.storage.indices.ProofListIndexProxy;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.core.util.LibraryLoader;
import com.exonum.core.messages.Blockchain.Config;
import com.exonum.core.messages.Runtime.ExecutionStatus;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A proxy class for the blockchain::Schema struct maintained by Exonum core.
 * Please refer to the
 * <a href="https://docs.rs/exonum/latest/exonum/blockchain/struct.Schema.html">doc</a> for details.
 */
final class CoreSchemaProxy {

  static {
    LibraryLoader.load();
  }

  private final NativeHandle nativeHandle;
  private final View dbView;
  private static final Serializer<Block> BLOCK_SERIALIZER = BlockSerializer.INSTANCE;
  private static final Serializer<TransactionLocation> TRANSACTION_LOCATION_SERIALIZER =
      TransactionLocationSerializer.INSTANCE;
  private static final Serializer<ExecutionStatus> EXECUTION_STATUS_SERIALIZER =
      protobuf(ExecutionStatus.class);
  private static final Serializer<TransactionMessage> TRANSACTION_MESSAGE_SERIALIZER =
      StandardSerializers.transactionMessage();
  private static final Serializer<Config> CONSENSUS_CONFIG_SERIALIZER =
      StandardSerializers.protobuf(Config.class);

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
        CoreIndex.BLOCKS, dbView, StandardSerializers.hash(), BLOCK_SERIALIZER);
  }

  /**
   * Returns the latest committed block.
   *
   * @throws RuntimeException if the "genesis block" was not created
   */
  Block getLastBlock() {
    return BLOCK_SERIALIZER.fromBytes(nativeGetLastBlock(nativeHandle.get()));
  }

  /**
   * Returns a map of transaction messages identified by their SHA-256 hashes.
   */
  MapIndex<HashCode, TransactionMessage> getTxMessages() {
    return MapIndexProxy.newInstance(CoreIndex.TRANSACTIONS, dbView, StandardSerializers.hash(),
        TRANSACTION_MESSAGE_SERIALIZER);
  }

  /**
   * Returns a map with a key-value pair of a transaction hash and execution result. Note that this
   * is a <a href="ProofMapIndexProxy.html#key-hashing">proof map that uses non-hashed keys</a>.
   */
  ProofMapIndexProxy<HashCode, ExecutionStatus> getTxResults() {
    return ProofMapIndexProxy.newInstanceNoKeyHashing(CoreIndex.TRANSACTIONS_RESULTS, dbView,
        StandardSerializers.hash(), EXECUTION_STATUS_SERIALIZER);
  }

  /**
   * Returns a map that keeps the block height and transaction position inside the block for every
   * transaction hash.
   */
  MapIndex<HashCode, TransactionLocation> getTxLocations() {
    return MapIndexProxy.newInstance(CoreIndex.TRANSACTIONS_LOCATIONS, dbView,
        StandardSerializers.hash(), TRANSACTION_LOCATION_SERIALIZER);
  }

  /**
   * Returns a set of uncommitted (in-pool) transaction hashes; empty in case of no transactions.
   * Note that this pool represents the state as of the current snapshot, and its state is volatile
   * even between block commits.
   *
   * @see <a href="https://exonum.com/doc/version/0.12/advanced/consensus/specification/#pool-of-unconfirmed-transactions">Pool of Unconfirmed Transactions</a>
   */
  KeySetIndexProxy<HashCode> getTransactionPool() {
    return KeySetIndexProxy.newInstance(CoreIndex.TRANSACTIONS_POOL, dbView,
        StandardSerializers.hash());
  }

  /**
   * Returns the current consensus configuration of the network.
   *
   * @throws IllegalStateException if the "genesis block" was not created
   */
  Config getConsensusConfiguration() {
    EntryIndexProxy<Config> configEntry = EntryIndexProxy.newInstance(CoreIndex.CONSENSUS_CONFIG,
        dbView, CONSENSUS_CONFIG_SERIALIZER);
    checkState(configEntry.isPresent(), "No consensus configuration: requesting the configuration "
        + "before the genesis block was created");
    return configEntry.get();
  }

  private static native long nativeCreate(long viewNativeHandle);

  private static native void nativeFree(long nativeHandle);

  private static native long nativeGetHeight(long nativeHandle);

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
    private static final String TRANSACTIONS_POOL = PREFIX + "transactions_pool";
    private static final String CONSENSUS_CONFIG = PREFIX + "consensus_config";
  }
}

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
import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.common.blockchain.Block;
import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.common.proofs.map.UncheckedMapProof;
import com.exonum.binding.common.serialization.BlockSerializer;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.common.serialization.TransactionLocationSerializer;
import com.exonum.binding.common.serialization.TransactionResultSerializer;
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
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * A proxy class for the blockchain::Schema struct maintained by Exonum core.
 * Please refer to the
 * <a href="https://docs.rs/exonum/latest/exonum/blockchain/struct.Schema.html">doc</a> for details.
 */
final class CoreSchemaProxy {

  private final NativeHandle nativeHandle;
  private final View dbView;
  private final Serializer<Block> blockSerializer = BlockSerializer.INSTANCE;
  private final Serializer<TransactionLocation> transactionLocationSerializer =
      TransactionLocationSerializer.INSTANCE;
  private final Serializer<TransactionResult> transactionResultSerializer =
      TransactionResultSerializer.INSTANCE;

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
    byte[] id = fixed64().toBytes(height);
    return ProofListIndexProxy.newInGroupUnsafe(
        CoreIndex.BLOCK_TRANSACTIONS, id, dbView, StandardSerializers.hash());
  }

  /**
   * Returns a table that stores a block object for every block hash.
   * @return a map with block object for every block hash
   */
  MapIndex<HashCode, Block> getBlocks() {
    return MapIndexProxy.newInstance(
        CoreIndex.BLOCKS, dbView, StandardSerializers.hash(), blockSerializer);
  }

  /**
   * Returns the latest committed block.
   * @return the latest committed block object
   * @throws RuntimeException if the "genesis block" was not created
   */
  Block getLastBlock() {
    return blockSerializer.fromBytes(nativeGetLastBlock(nativeHandle.get()));
  }

  /**
   * Returns a table that represents a map with a key-value pair of a
   * transaction hash and transaction message.
   * @return a map with a key-value pair of a transaction hash and transaction message
   */
  MapIndex<HashCode, TransactionMessage> getTxMessages() {
    // TODO: serializer
    return MapIndexProxy.newInstance(CoreIndex.TRANSACTIONS, dbView, StandardSerializers.hash(),
        null);
  }

  /**
   * Returns a table that represents a map with a key-value pair of a transaction
   * hash and execution result.
   * @return a map with a key-value pair of a transaction hash and execution result
   */
  ProofMapIndexProxy<HashCode, TransactionResult> getTxResults() {
    return ProofMapIndexProxy.newInstance(CoreIndex.TRANSACTIONS_RESULTS, dbView,
        StandardSerializers.hash(), transactionResultSerializer);
  }

  /**
   * Returns a table that keeps the block height and transaction position inside the block for every
   * transaction hash.
   * @return a map with transaction position for every transaction hash
   */
  MapIndex<HashCode, TransactionLocation> getTxLocations() {
    return MapIndexProxy.newInstance(CoreIndex.TRANSACTIONS_LOCATIONS, dbView,
        StandardSerializers.hash(), transactionLocationSerializer);
  }

  @SuppressWarnings("unused") // TODO: should be implemented later
  UncheckedMapProof getProofToServiceCollection(short serviceId, int collectionIndex) {
    throw new NotImplementedException();
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

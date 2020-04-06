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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.common.blockchain.CallInBlocks;
import com.exonum.binding.common.blockchain.ExecutionStatuses;
import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.blockchain.proofs.BlockProof;
import com.exonum.binding.core.blockchain.proofs.IndexProof;
import com.exonum.binding.core.service.Configuration;
import com.exonum.binding.core.service.ExecutionContext;
import com.exonum.binding.core.storage.database.Access;
import com.exonum.binding.core.storage.indices.KeySetIndexProxy;
import com.exonum.binding.core.storage.indices.ListIndex;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.core.storage.indices.ProofListIndexProxy;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.messages.core.Blockchain.CallInBlock;
import com.exonum.messages.core.Blockchain.Config;
import com.exonum.messages.core.Proofs;
import com.exonum.messages.core.runtime.Errors.ExecutionError;
import com.exonum.messages.core.runtime.Errors.ExecutionStatus;
import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;

/**
 * Provides read-only access to the subset of
 * <a href="https://docs.rs/exonum/latest/exonum/blockchain/struct.Schema.html">
 * blockchain::Schema</a> features in the Core API: blocks, transaction messages, execution
 * results.
 *
 * <!-- This section is supposed to be the main Javadoc on proofs, documenting how
 *      to create various blockchain proofs from their components.
 *
 *      Link here with <a href="<relative path>/Blockchain.html#proofs">Blockchain Proofs</a>.
 *      See also: https://stackoverflow.com/a/27522316/ -->
 * <h2 id="proofs">Proofs</h2>
 *
 * <p>Blockchain allows creating cryptographic proofs that some data is indeed stored
 * in the database. Exonum supports the following types of proofs:
 * <ul>
 *   <li>Block Proof</li>
 *   <li>Transaction Execution Proof</li>
 *   <li>Call Result Proof</li>
 *   <li>Service Data Proof</li>
 * </ul>
 *
 * <h3 id="block-proof">Block Proof</h3>
 *
 * <p>A block proof proves correctness of a blockchain block. It can be created with
 * {@link #createBlockProof(long)} for any committed block. See also {@link BlockProof}.
 *
 * <h3 id="tx-execution-proof">Transaction Execution Proof</h3>
 *
 * <p>A transaction execution proof proves that a transaction with a given message hash was
 * executed in a block at a certain height at a certain
 * <em>{@linkplain TransactionLocation location}</em>. It consists of a block proof,
 * and a list proof from {@link #getBlockTransactions(long)}. It may be extended to
 * a <em>call result</em> proof — read the next section.
 *
 * <h3 id="call-result-proof">Call Result Proof</h3>
 *
 * <p>A call result proof proves that a given service call completed with a particular
 * result in a block at a certain height. It consists of a block proof and a map proof
 * from the call errors registry, which {@linkplain ProofMapIndexProxy#getIndexHash() index hash}
 * is recorded in the block header as {@link Block#getErrorHash()}.
 *
 * <p>To construct such a proof, access the call records for a particular block with
 * {@link #getCallRecords(long)}; and use {@link CallRecords#getProof(CallInBlock)}.
 *
 * <h3 id="service-data-proof">Service Data Proof</h3>
 *
 * <p>A service data proof proves that some service index contains certain data as of the last
 * committed block. It includes:
 * <ul>
 *   <li>An index proof: a block proof + a proof from the aggregating collection.</li>
 *   <li>A proof from the service index.</li>
 * </ul>
 *
 * <p>An index proof is created with {@link #createIndexProof(String)}.
 *
 * <h4>Example</h4>
 *
 * <p>Consider a simple timestamping service that keeps timestamps for event ids, and supports
 * proofs of their authenticity.
 *
 * <p>First, create a message definition for a proof:
 *
 * <pre>
 *   message TimestampProof {
 *     MapProof timestamp = 1;
 *     IndexProof indexProof = 2;
 *   }
 * </pre>
 *
 * <p>Then create the two components: timestamp proof from a service index and index proof
 * for that index from the blockchain:
 *
 * <pre>
 *   TimestampProof createTimestampProof(Snapshot s,
 *                                       String eventId) {
 *     // 1. Create a timestamp proof
 *     // The literal is for illustrative purposes —
 *     // usually the service name is prepended elsewhere
 *     var fullIndexName = "timestamping.timestamp";
 *     var timestamps = ProofMapIndexProxy.newInstance(fullIndexName, s,
 *         string(), timestamp());
 *     var tsProof = timestamps.getProof(eventId);
 *
 *     // 2. Create an index proof
 *     var blockchain = Blockchain.newInstance(s);
 *     var indexProof = blockchain.createIndexProof(fullIndexName);
 *
 *     // 3. Create a complete service data proof
 *     return TimestampProof.newBuilder()
 *       .setTimestamp(tsProof.getAsMessage())
 *       .setIndexProof(indexProof.getAsMessage())
 *       .build();
 *   }
 * </pre>
 *
 * <p>Finally, serialize the proof and send it to the client.
 *
 * <hr/>
 *
 * <p>All method arguments are non-null by default.
 * <!-- TODO: Link a page on proofs from exonum.com when one arrives: ECR-4106 -->
 */
public final class Blockchain {

  private final Access access;
  private final CoreSchema schema;

  @VisibleForTesting
  Blockchain(Access access, CoreSchema schema) {
    this.access = access;
    this.schema = schema;
  }

  /**
   * Constructs a new blockchain instance for the given database access.
   *
   * <p>Expects a non-prefixed access. Use {@link BlockchainData#getBlockchain()} in service code.
   */
  public static Blockchain newInstance(Access access) {
    CoreSchema coreSchema = CoreSchema.newInstance(access);
    return new Blockchain(access, coreSchema);
  }

  /**
   * Creates a proof for the block at the given height.
   *
   * <p>It allows creating genesis block proofs, but they make little sense, as a genesis
   * block is supposed to be a "root of trust", hence, well-known to the clients verifying
   * any subsequent proofs coming from the blockchain.
   *
   * <p>If you need to create a proof for a service index, use {@link #createIndexProof(String)}.
   * @param blockHeight a height of the block for which to create a proof
   * @throws IndexOutOfBoundsException if the height is not valid
   * @see #createIndexProof(String)
   */
  public BlockProof createBlockProof(long blockHeight) {
    checkHeight(blockHeight);
    Proofs.BlockProof blockProof = BlockchainProofs.createBlockProof(access, blockHeight);
    return BlockProof.newInstance(blockProof);
  }

  /**
   * Creates a proof for a single index in the database. It is usually a part
   * of a <a href="#service-data-proof">Service Data Proof</a>.
   *
   * @param fullIndexName the full index name for which to create a proof
   * @throws IllegalStateException if the access is not a snapshot, because a state of a service
   *     index can be proved only for the latest committed block, not for any intermediate state
   *     during transaction processing
   * @throws IllegalArgumentException if the index with the given name does not exist;
   *     or is not Merkelized. An index does not exist until it is <em>initialized</em> —
   *     created for the first time
   *     with a {@link com.exonum.binding.core.storage.database.Fork}. Depending on the service
   *     logic, an index may remain uninitialized indefinitely. Therefore, if proofs for an
   *     empty index need to be created, it must be initialized early in the service lifecycle
   *     (e.g., in {@link
   *     com.exonum.binding.core.service.Service#initialize(ExecutionContext, Configuration)}.
   *     <!-- TODO: Simplify once initialization happens automatically: ECR-4121 -->
   */
  public IndexProof createIndexProof(String fullIndexName) {
    checkState(!access.canModify(), "Cannot create an index proof for a mutable access (%s).",
        access);
    return BlockchainProofs.createIndexProof(access, fullIndexName)
        .map(IndexProof::newInstance)
        .orElseThrow(() -> new IllegalArgumentException(
            String.format("Index %s does not exist or is not Merkelized", fullIndexName)));
  }

  /**
   * Returns true if the blockchain contains <em>exactly</em> the same block as the passed
   * value; false if it does not contain such block. Please note that all block fields
   * are compared, not only its hash.
   *
   * @param block a value to check for presence in the blockchain
   */
  public boolean containsBlock(Block block) {
    return findBlock(block.getBlockHash())
        .map(block::equals)
        .orElse(false);
  }

  /**
   * Returns the <em>blockchain height</em> which is the height of the latest committed block
   * in the blockchain. The block height is a distance between the last block
   * and the "genesis", or initial, block. Therefore, the blockchain height is equal to the number
   * of blocks plus one.
   *
   * <p>For example, the "genesis" block has height {@code h = 0}. The latest committed block
   * has height {@code h = getBlockHashes().size() - 1}.
   *
   * @throws RuntimeException if the "genesis block" was not created yet;
   *     consider using {@link #getNextHeight()} in service methods that might be invoked
   *     before the genesis block commit
   */
  public long getHeight() {
    return schema.getHeight();
  }

  /**
   * Returns the blockchain height of the <em>next</em> block to be committed.
   *
   * @see #getHeight()
   */
  public long getNextHeight() {
    return getBlockHashes().size();
  }

  /**
   * Returns a list of all block hashes, indexed by the block height.
   * For example, the "genesis block" will be at index 0,
   * the block at height {@code h = 10} — at index 10.
   * The last committed block will be at height {@code h = getBlockHashes().size() - 1}.
   */
  public ListIndex<HashCode> getBlockHashes() {
    return schema.getBlockHashes();
  }

  /**
   * Returns a proof list of transaction hashes committed in the block at the given height.
   *
   * <p>The {@linkplain ProofListIndexProxy#getIndexHash() index hash} of this index is recorded
   * in the block header as {@link Block#getTxRootHash()}. That allows constructing
   * <a href="Blockchain.html#tx-execution-proof">proofs</a> that a transaction
   * with a certain message hash was executed at a certain
   * <em>{@linkplain TransactionLocation location}</em>: (block_height, tx_index_in_block) pair.
   *
   * @param height block height starting from 0
   * @throws IllegalArgumentException if the height is invalid: negative or exceeding
   *     the {@linkplain #getHeight() blockchain height}
   */
  public ProofListIndexProxy<HashCode> getBlockTransactions(long height) {
    return schema.getBlockTransactions(height);
  }

  /**
   * Returns a proof list of transaction hashes committed in the block with the given id.
   *
   * @param blockId id of the block
   * @throws IllegalArgumentException if there is no block with given id
   * @see #getBlockTransactions(long)
   */
  public ProofListIndexProxy<HashCode> getBlockTransactions(HashCode blockId) {
    Optional<Block> block = findBlock(blockId);
    checkArgument(block.isPresent(), "No block found for given id %s", blockId);
    return getBlockTransactions(block.get().getHeight());
  }

  /**
   * Returns a proof list of transaction hashes committed in the given block.
   * The given block must match exactly the block that is stored in the database.
   *
   * @param block block of which list of transaction hashes should be returned
   * @throws IllegalArgumentException if there is no such block in the blockchain
   * @see #getBlockTransactions(long)
   */
  public ProofListIndexProxy<HashCode> getBlockTransactions(Block block) {
    checkArgument(containsBlock(block), "No such block (%s) in the database", block);
    return getBlockTransactions(block.getHeight());
  }

  /**
   * Returns a map of transaction messages identified by their SHA-256 hashes. Both committed and
   * in-pool (not yet processed) transactions are returned.
   */
  public MapIndex<HashCode, TransactionMessage> getTxMessages() {
    return schema.getTxMessages();
  }

  /**
   * Returns the call records, containing the execution errors that occurred in the block
   * at the given height.
   *
   * @param blockHeight the height of the block
   * @throws IllegalArgumentException if the height is invalid: negative or exceeding
   *     the {@linkplain #getHeight() blockchain height}
   */
  public CallRecords getCallRecords(long blockHeight) {
    return new CallRecords(schema, this, blockHeight);
  }

  /**
   * Returns a transaction execution result for the given message hash.
   *
   * @return a transaction execution result, or {@code Optional.empty()} if this transaction
   *         is unknown or was not yet executed
   */
  public Optional<ExecutionStatus> getTxResult(HashCode messageHash) {
    return getTxLocation(messageHash)
        .map(this::getExecutionStatus);
  }

  private ExecutionStatus getExecutionStatus(TransactionLocation txLocation) {
    long height = txLocation.getHeight();
    var callRecords = getCallRecords(height);
    var txCallId = CallInBlocks.transaction(txLocation.getIndexInBlock());
    var txErrorOpt = callRecords.get(txCallId);
    if (txErrorOpt.isPresent()) {
      ExecutionError txError = txErrorOpt.get();
      return ExecutionStatus.newBuilder()
          .setError(txError)
          .build();
    }
    // No error: tx completed successfully
    return ExecutionStatuses.SUCCESS;
  }

  /**
   * Returns a map that keeps the transaction position inside the blockchain for every transaction
   * hash.
   */
  public MapIndex<HashCode, TransactionLocation> getTxLocations() {
    return schema.getTxLocations();
  }

  /**
   * Returns transaction position inside the blockchain for given message hash.
   *
   * @return a transaction execution result, or {@code Optional.empty()} if this transaction
   *         is unknown or was not yet executed
   */
  public Optional<TransactionLocation> getTxLocation(HashCode messageHash) {
    MapIndex<HashCode, TransactionLocation> txLocations = getTxLocations();
    TransactionLocation transactionLocation = txLocations.get(messageHash);
    return Optional.ofNullable(transactionLocation);
  }

  /**
   * Returns a map that stores a block object for every block hash.
   */
  public MapIndex<HashCode, Block> getBlocks() {
    return schema.getBlocks();
  }

  /**
   * Returns the block at the given height.
   *
   * @param height the height of the block; must be non-negative and less than or equal to
   *     the current {@linkplain #getHeight() blockchain height}
   * @return a block at the height
   * @throws IndexOutOfBoundsException if the height is not valid
   */
  public Block getBlock(long height) {
    checkHeight(height);

    ListIndex<HashCode> blockHashes = getBlockHashes();
    HashCode blockHash = blockHashes.get(height);
    MapIndex<HashCode, Block> blocks = getBlocks();
    return blocks.get(blockHash);
  }

  /** Checks if the blockchain height is valid. */
  private void checkHeight(long height) {
    long blockchainHeight = getHeight();
    if (height < 0 || height > blockchainHeight) {
      throw new IndexOutOfBoundsException("Block height (" + height + ") is out of range [0, "
          + blockchainHeight + "]");
    }
  }

  /**
   * Returns a block object for given block hash.
   *
   * @return a corresponding block, or {@code Optional.empty()} if there is no block with given
   *         block hash
   */
  public Optional<Block> findBlock(HashCode blockHash) {
    MapIndex<HashCode, Block> blocks = getBlocks();
    Block block = blocks.get(blockHash);
    return Optional.ofNullable(block);
  }

  /**
   * Returns the latest committed block.
   *
   * @throws IllegalStateException if the "genesis block" was not created
   */
  public Block getLastBlock() {
    ListIndex<HashCode> blockHashes = getBlockHashes();
    checkState(!blockHashes.isEmpty(),
        "No genesis block created yet (block hashes list is empty)");
    HashCode lastBlockHash = blockHashes.getLast();
    return getBlocks().get(lastBlockHash);
  }

  /**
   * Returns the current consensus configuration of the network.
   *
   * @throws IllegalStateException if the "genesis block" was not created
   * @see <a href="https://exonum.com/doc/version/1.0.0/architecture/configuration/">Exonum configuration</a> for
   *     consensus configuration information.
   */
  public Config getConsensusConfiguration() {
    return schema.getConsensusConfiguration();
  }

  /**
   * Returns a set of uncommitted (in-pool) transaction hashes; empty in case of no transactions.
   * Note that this pool represents the state as of the current snapshot, and its state is volatile
   * even between block commits.
   *
   * @see <a href="https://exonum.com/doc/version/1.0.0/advanced/consensus/specification/#pool-of-unconfirmed-transactions">Pool of Unconfirmed Transactions</a>
   */
  public KeySetIndexProxy<HashCode> getTransactionPool() {
    return schema.getTransactionPool();
  }

  /**
   * Returns the total number of transactions committed to the blockchain.
   */
  public long getNumTransactions() {
    return schema.getNumTransactions().orElse(0L);
  }
}

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

package com.exonum.binding.blockchain;

import static com.exonum.binding.blockchain.Blocks.withProperHash;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.exonum.binding.blockchain.Block.Builder;
import com.exonum.binding.blockchain.serialization.BlockSerializer;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.HashFunction;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.indices.ListIndex;
import com.exonum.binding.storage.indices.ListIndexProxy;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.MapIndexProxy;
import com.exonum.binding.storage.indices.ProofListIndexProxy;
import com.exonum.binding.test.RequiresNativeLibrary;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@RequiresNativeLibrary
@ExtendWith(MockitoExtension.class)
class BlockchainIntegrationTest {

  private static final String TEST_BLOCKS = "test_blocks";
  private static final String TEST_BLOCK_HASHES = "test_block_hashes";
  private static final String TEST_BLOCK_TRANSACTIONS = "test_block_transactions";

  @Mock
  CoreSchemaProxy coreSchema;

  private Blockchain blockchain;

  private MemoryDb database;

  @BeforeEach
  void setUp() {
    blockchain = new Blockchain(coreSchema);
    database = MemoryDb.newInstance();
  }

  @AfterEach
  void tearDown() {
    database.close();
  }

  @Nested
  class WithSingleBlock {
    final List<HashCode> expectedBlockTransactions = ImmutableList.of(HashCode.fromInt(1));

    Cleaner cleaner;
    Block block;

    @BeforeEach
    void setUp() {
      cleaner = new Cleaner();
      Fork fork = database.createFork(cleaner);

      // Setup blocks
      MapIndex<HashCode, Block> blocks = MapIndexProxy.newInstance(TEST_BLOCKS, fork,
          StandardSerializers.hash(), BlockSerializer.INSTANCE);
      int blockHeight = 1;
      block = withProperHash(aBlock(blockHeight)
          .numTransactions(expectedBlockTransactions.size())
          .build());
      blocks.put(block.getBlockHash(), block);
      when(coreSchema.getBlocks()).thenReturn(blocks);

      // Setup tx hashes
      ProofListIndexProxy<HashCode> transactionHashes = ProofListIndexProxy.newInstance(
          TEST_BLOCK_TRANSACTIONS, fork, StandardSerializers.hash());
      transactionHashes.addAll(expectedBlockTransactions);
      when(coreSchema.getBlockTransactions(blockHeight)).thenReturn(transactionHashes);
    }

    @AfterEach
    void tearDown() throws CloseFailuresException {
      cleaner.close();
    }

    @Test
    void containsBlock() {
      assertTrue(blockchain.containsBlock(block));
    }

    @Test
    void containsBlockNoSuchBlock() {
      Block unknownBlock = aBlock(Long.MAX_VALUE).build();
      assertFalse(blockchain.containsBlock(unknownBlock));
    }

    @Test
    void containsBlockSameHashDistinctFields() {
      // Check against a block that has the same hash, but different fields.
      Block unknownBlock = aBlock(10L)
          .blockHash(block.getBlockHash())
          .build();
      assertFalse(blockchain.containsBlock(unknownBlock));
    }

    @Test
    void getBlockTransactionsByBlock() {
      assertThat(blockchain.getBlockTransactions(block))
          .hasSameElementsAs(expectedBlockTransactions);
    }

    @Test
    void getBlockTransactionsByBlockNoSuchBlock() {
      Block unknownBlock = aBlock(Long.MAX_VALUE).build();

      Exception e = assertThrows(IllegalArgumentException.class,
          () -> blockchain.getBlockTransactions(unknownBlock));

      String hashAsString = unknownBlock.getBlockHash().toString();
      assertThat(e).hasMessageContaining(hashAsString);
    }

    @Test
    void getBlockTransactionsByBlockSameHashDistinctFields() {
      // Check against a block that has the same hash, but different fields.
      Block unknownBlock = aBlock(10L)
          .blockHash(block.getBlockHash())
          .build();

      Exception e = assertThrows(IllegalArgumentException.class,
          () -> blockchain.getBlockTransactions(unknownBlock));

      String hashAsString = unknownBlock.getBlockHash().toString();
      assertThat(e).hasMessageContaining(hashAsString);
    }
  }

  @Nested
  class WithSeveralBlocks {

    private static final long HEIGHT = 2;

    Cleaner cleaner;
    List<Block> blocks;

    @BeforeEach
    void setUp() throws CloseFailuresException {
      // Fill database with test data
      blocks = createTestBlockchain(HEIGHT);

      // Create a cleaner for Snapshot
      cleaner = new Cleaner();
      Snapshot snapshot = database.createSnapshot(cleaner);

      // Setup mocks
      setupMocks(snapshot);
    }

    @AfterEach
    void tearDown() throws CloseFailuresException {
      cleaner.close();
    }

    private List<Block> createTestBlockchain(long blockchainHeight)
        throws CloseFailuresException {
      try (Cleaner cleaner = new Cleaner()) {
        List<Block> blocks = LongStream.rangeClosed(0, blockchainHeight)
            .mapToObj(h -> aBlock(h).build())
            .map(Blocks::withProperHash)
            .collect(Collectors.toList());

        // Add the blocks
        // … to the map of blocks
        Fork fork = database.createFork(cleaner);
        MapIndex<HashCode, Block> blocksByHash = MapIndexProxy.newInstance(TEST_BLOCKS, fork,
            StandardSerializers.hash(), BlockSerializer.INSTANCE);

        for (Block b : blocks) {
          blocksByHash.put(b.getBlockHash(), b);
        }

        // … to the list of block hashes
        ListIndex<HashCode> blockHashes = ListIndexProxy.newInstance(TEST_BLOCK_HASHES, fork,
            StandardSerializers.hash());
        for (Block b : blocks) {
          blockHashes.add(b.getBlockHash());
        }

        // Merge the changes
        database.merge(fork);

        return blocks;
      }
    }

    private void setupMocks(Snapshot snapshot) {
      when(coreSchema.getHeight()).thenReturn(HEIGHT);
      MapIndex<HashCode, Block> blocksByHash = MapIndexProxy.newInstance(TEST_BLOCKS, snapshot,
          StandardSerializers.hash(), BlockSerializer.INSTANCE);
      when(coreSchema.getBlocks()).thenReturn(blocksByHash);
      ListIndex<HashCode> blockHashes = ListIndexProxy.newInstance(TEST_BLOCK_HASHES, snapshot,
          StandardSerializers.hash());
      when(coreSchema.getBlockHashes()).thenReturn(blockHashes);
    }

    @Test
    void getBlockAtHeight() {
      long blockchainHeight = HEIGHT;

      // Test that correct blocks are returned
      for (int height = 0; height <= blockchainHeight; height++) {
        Block actualBlock = blockchain.getBlock(height);
        Block expectedBlock = blocks.get(height);

        assertThat(actualBlock).isEqualTo(expectedBlock);
      }
      // Test no blocks beyond the height
      assertThrows(IndexOutOfBoundsException.class,
          () -> blockchain.getBlock(blockchainHeight + 2));
    }

    @ParameterizedTest
    @ValueSource(longs = {
        Long.MIN_VALUE, -1, HEIGHT + 1, HEIGHT + 2, Long.MAX_VALUE
    })
    void getBlockAtInvalidHeight(long blockchainHeight) {
      Exception e = assertThrows(IndexOutOfBoundsException.class,
          () -> blockchain.getBlock(blockchainHeight + 1));

      assertThat(e).hasMessageContaining(Long.toString(blockchainHeight));
    }
  }

  /**
   * Creates a builder of a block, fully initialized with defaults, inferred from the height.
   * An invocation with the same height will produce exactly the same builder. The block hash
   * is <strong>not</strong> equal to the hash of the block with such parameters.
   *
   * @param blockHeight a block height
   * @return a new block builder
   */
  private static Builder aBlock(long blockHeight) {
    HashFunction hashFunction = Hashing.sha256();
    return Block.builder()
        .proposerId(0)
        .height(blockHeight)
        .numTransactions(0)
        .blockHash(hashFunction.hashLong(blockHeight))
        .previousBlockHash(hashFunction.hashLong(blockHeight - 1))
        .txRootHash(hashFunction.hashString("transactions at" + blockHeight, UTF_8))
        .stateHash(hashFunction.hashString("state hash at " + blockHeight, UTF_8));
  }

}

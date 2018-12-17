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
import com.exonum.binding.test.RequiresNativeLibrary;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@RequiresNativeLibrary
@ExtendWith(MockitoExtension.class)
class BlockchainIntegrationTest {

  private static final String TEST_BLOCKS = "test_blocks";
  private static final String TEST_BLOCK_HASHES = "test_block_hashes";

  @Mock
  CoreSchemaProxy coreSchema;

  Blockchain blockchain;

  @BeforeEach
  void setUp() {
    blockchain = new Blockchain(coreSchema);
  }

  @Test
  void containsBlock() throws Exception {
    try (MemoryDb database = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork fork = database.createFork(cleaner);
      MapIndex<HashCode, Block> blocks = MapIndexProxy.newInstance(TEST_BLOCKS, fork,
          StandardSerializers.hash(), BlockSerializer.INSTANCE);
      when(coreSchema.getBlocks()).thenReturn(blocks);

      Block b = withProperHash(aBlock(1).build());

      blocks.put(b.getBlockHash(), b);

      assertTrue(blockchain.containsBlock(b));
    }
  }

  @Test
  void containsBlockNoSuchBlock() throws Exception {
    try (MemoryDb database = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork fork = database.createFork(cleaner);
      MapIndex<HashCode, Block> blocks = MapIndexProxy.newInstance(TEST_BLOCKS, fork,
          StandardSerializers.hash(), BlockSerializer.INSTANCE);
      when(coreSchema.getBlocks()).thenReturn(blocks);

      Block b = withProperHash(aBlock(1).build());
      blocks.put(b.getBlockHash(), b);

      Block unknownBlock = withProperHash(aBlock(Long.MAX_VALUE).build());
      assertFalse(blockchain.containsBlock(unknownBlock));
    }
  }

  @Test
  void containsBlockSameHashDistinctFields() throws Exception {
    try (MemoryDb database = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      Fork fork = database.createFork(cleaner);
      MapIndex<HashCode, Block> blocks = MapIndexProxy.newInstance(TEST_BLOCKS, fork,
          StandardSerializers.hash(), BlockSerializer.INSTANCE);
      when(coreSchema.getBlocks()).thenReturn(blocks);

      Block b = withProperHash(aBlock(1).build());
      blocks.put(b.getBlockHash(), b);

      // Check against a block that has the same hash, but different fields.
      Block unknownBlock = aBlock(10L)
          .blockHash(b.getBlockHash())
          .build();
      assertFalse(blockchain.containsBlock(unknownBlock));
    }
  }

  @Test
  void getBlockAtHeight() throws Exception {
    try (MemoryDb database = MemoryDb.newInstance();
        Cleaner cleaner = new Cleaner()) {
      long blockchainHeight = 2;
      List<Block> blocks = createTestBlockchain(database, blockchainHeight);

      // Setup mocks
      when(coreSchema.getHeight()).thenReturn(blockchainHeight);
      Snapshot snapshot = database.createSnapshot(cleaner);
      MapIndex<HashCode, Block> blocksByHash = MapIndexProxy.newInstance(TEST_BLOCKS, snapshot,
          StandardSerializers.hash(), BlockSerializer.INSTANCE);
      when(coreSchema.getBlocks()).thenReturn(blocksByHash);
      ListIndex<HashCode> blockHashes = ListIndexProxy.newInstance(TEST_BLOCK_HASHES, snapshot,
          StandardSerializers.hash());
      when(coreSchema.getBlockHashes()).thenReturn(blockHashes);

      // Test that correct blocks are returned
      for (int height = 0; height <= blockchainHeight; height++) {
        Block actualBlock = blockchain.getBlock(height);
        Block expectedBlock = blocks.get(height);

        assertThat(actualBlock).isEqualTo(expectedBlock);
      }
      // Test no blocks beyond the height
      assertThrows(IndexOutOfBoundsException.class,
          () -> blockchain.getBlock(blockchainHeight + 1));
      assertThrows(IndexOutOfBoundsException.class,
          () -> blockchain.getBlock(blockchainHeight + 2));
    }
  }

  private List<Block> createTestBlockchain(MemoryDb database, long blockchainHeight)
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

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.blockchain.Block;
import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.configuration.StoredConfiguration;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.storage.indices.ListIndexProxy;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.ProofListIndexProxy;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("unchecked") // Don't warn of unchecked assignment of mocks
@ExtendWith(MockitoExtension.class)
class BlockchainTest {

  private static final long HEIGHT = 10L;

  private static final Block BLOCK = Block.builder()
      .proposerId(1)
      .height(HEIGHT)
      .numTransactions(1)
      .blockHash(HashCode.fromString("ab"))
      .previousBlockHash(HashCode.fromString("bc"))
      .txRootHash(HashCode.fromString("cd"))
      .stateHash(HashCode.fromString("ab"))
      .build();

  private Blockchain blockchain;

  @Mock
  private CoreSchemaProxy mockSchema;

  @BeforeEach
  void setUp() {
    blockchain = new Blockchain(mockSchema);
  }

  @Test
  void getHeight() {
    when(mockSchema.getHeight()).thenReturn(HEIGHT);

    assertThat(blockchain.getHeight()).isEqualTo(HEIGHT);
  }

  @Test
  void getAllBlockHashes() {
    ListIndexProxy blockHashes = mock(ListIndexProxy.class);
    when(mockSchema.getBlockHashes()).thenReturn(blockHashes);

    assertThat(blockchain.getBlockHashes()).isEqualTo(blockHashes);
  }

  @Test
  void getBlockTransactionsByHeight() {
    ProofListIndexProxy transactions = mock(ProofListIndexProxy.class);
    when(mockSchema.getBlockTransactions(HEIGHT)).thenReturn(transactions);

    assertThat(blockchain.getBlockTransactions(HEIGHT)).isEqualTo(transactions);
  }

  @Test
  void getBlockTransactionsByBlockId() {
    ProofListIndexProxy transactions = mock(ProofListIndexProxy.class);
    MapIndex blocks = mock(MapIndex.class);
    HashCode blockId = HashCode.fromString("ab");

    when(mockSchema.getBlocks()).thenReturn(blocks);
    when(blocks.get(blockId)).thenReturn(BLOCK);
    when(mockSchema.getBlockTransactions(HEIGHT)).thenReturn(transactions);

    assertThat(blockchain.getBlockTransactions(blockId)).isEqualTo(transactions);
  }

  @Test
  void getTxMessages() {
    MapIndex txMessages = mock(MapIndex.class);
    when(mockSchema.getTxMessages()).thenReturn(txMessages);

    assertThat(blockchain.getTxMessages()).isEqualTo(txMessages);
  }

  @Test
  void getTxResults() {
    ProofMapIndexProxy txResults = mock(ProofMapIndexProxy.class);
    when(mockSchema.getTxResults()).thenReturn(txResults);

    assertThat(blockchain.getTxResults()).isEqualTo(txResults);
  }

  @Test
  void getTxResult() {
    ProofMapIndexProxy txResults = mock(ProofMapIndexProxy.class);
    HashCode messageHash = HashCode.fromString("ab");
    TransactionResult txResult = TransactionResult.successful();

    when(txResults.get(messageHash)).thenReturn(txResult);
    when(mockSchema.getTxResults()).thenReturn(txResults);

    assertThat(blockchain.getTxResult(messageHash).get()).isEqualTo(txResult);
  }

  @Test
  void getNonexistentTxResult() {
    ProofMapIndexProxy txResults = mock(ProofMapIndexProxy.class);
    HashCode messageHash = HashCode.fromString("ab");

    when(txResults.get(messageHash)).thenReturn(null);
    when(mockSchema.getTxResults()).thenReturn(txResults);

    assertThat(blockchain.getTxResult(messageHash)).isEmpty();
  }

  @Test
  void getTxLocations() {
    MapIndex txLocations = mock(MapIndex.class);
    when(mockSchema.getTxLocations()).thenReturn(txLocations);

    assertThat(blockchain.getTxLocations()).isEqualTo(txLocations);
  }

  @Test
  void getTxLocation() {
    MapIndex txLocations = mock(MapIndex.class);
    HashCode messageHash = HashCode.fromString("ab");
    TransactionLocation txLocation = TransactionLocation.valueOf(1L, 1L);

    when(txLocations.get(messageHash)).thenReturn(txLocation);
    when(mockSchema.getTxLocations()).thenReturn(txLocations);

    assertThat(blockchain.getTxLocation(messageHash).get()).isEqualTo(txLocation);
  }

  @Test
  void getNonexistentTxLocation() {
    MapIndex blocks = mock(MapIndex.class);
    HashCode messageHash = HashCode.fromString("ab");

    when(blocks.get(messageHash)).thenReturn(null);
    when(mockSchema.getTxLocations()).thenReturn(blocks);

    assertThat(blockchain.getTxLocation(messageHash)).isEmpty();
  }

  @Test
  void getBlocks() {
    MapIndex mockMapIndex = mock(MapIndex.class);
    when(mockSchema.getBlocks()).thenReturn(mockMapIndex);

    assertThat(blockchain.getBlocks()).isEqualTo(mockMapIndex);
  }

  @Test
  void findBlock() {
    MapIndex blocks = mock(MapIndex.class);
    HashCode blockHash = HashCode.fromString("ab");

    when(blocks.get(blockHash)).thenReturn(BLOCK);
    when(mockSchema.getBlocks()).thenReturn(blocks);

    assertThat(blockchain.findBlock(blockHash).get()).isEqualTo(BLOCK);
  }

  @Test
  void findNonexistentBlock() {
    MapIndex blocks = mock(MapIndex.class);
    HashCode blockHash = HashCode.fromString("ab");

    when(blocks.get(blockHash)).thenReturn(null);
    when(mockSchema.getBlocks()).thenReturn(blocks);

    assertThat(blockchain.findBlock(blockHash)).isEmpty();
  }

  @Test
  void getLastBlock() {
    when(mockSchema.getLastBlock()).thenReturn(BLOCK);

    assertThat(blockchain.getLastBlock()).isEqualTo(BLOCK);
  }

  @Test
  void getActualConfiguration() {
    StoredConfiguration configuration = mock(StoredConfiguration.class);
    when(mockSchema.getActualConfiguration()).thenReturn(configuration);

    assertThat(blockchain.getActualConfiguration()).isEqualTo(configuration);
  }
}

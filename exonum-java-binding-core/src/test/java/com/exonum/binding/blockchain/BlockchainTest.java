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
    ListIndexProxy mockListIndex = mock(ListIndexProxy.class);
    when(mockSchema.getAllBlockHashes()).thenReturn(mockListIndex);

    assertThat(blockchain.getAllBlockHashes()).isEqualTo(mockListIndex);
  }

  @Test
  void getBlockTransactionsByHeight() {
    ProofListIndexProxy mockListIndex = mock(ProofListIndexProxy.class);
    when(mockSchema.getBlockTransactions(HEIGHT)).thenReturn(mockListIndex);

    assertThat(blockchain.getBlockTransactions(HEIGHT)).isEqualTo(mockListIndex);
  }

  @Test
  void getBlockTransactionsByBlockId() {
    ProofListIndexProxy mockListIndex = mock(ProofListIndexProxy.class);
    MapIndex mockMapIndex = mock(MapIndex.class);
    HashCode blockId = HashCode.fromString("ab");

    when(mockSchema.getBlocks()).thenReturn(mockMapIndex);
    when(mockMapIndex.get(blockId)).thenReturn(BLOCK);
    when(mockSchema.getBlockTransactions(HEIGHT)).thenReturn(mockListIndex);

    assertThat(blockchain.getBlockTransactions(blockId)).isEqualTo(mockListIndex);
  }

  @Test
  void getBlockTransactionsByBlock() {
    ProofListIndexProxy mockListIndex = mock(ProofListIndexProxy.class);
    when(mockSchema.getBlockTransactions(HEIGHT)).thenReturn(mockListIndex);

    assertThat(blockchain.getBlockTransactions(BLOCK)).isEqualTo(mockListIndex);
  }

  @Test
  void getTxMessages() {
    MapIndex mockMapIndex = mock(MapIndex.class);
    when(mockSchema.getTxMessages()).thenReturn(mockMapIndex);

    assertThat(blockchain.getTxMessages()).isEqualTo(mockMapIndex);
  }

  @Test
  void getTxResults() {
    ProofMapIndexProxy mockMapIndex = mock(ProofMapIndexProxy.class);
    when(mockSchema.getTxResults()).thenReturn(mockMapIndex);

    assertThat(blockchain.getTxResults()).isEqualTo(mockMapIndex);
  }

  @Test
  void getTxResult() {
    ProofMapIndexProxy mockMapIndex = mock(ProofMapIndexProxy.class);
    HashCode messageHash = HashCode.fromString("ab");
    TransactionResult txResult = TransactionResult.successful();

    when(mockMapIndex.get(messageHash)).thenReturn(txResult);
    when(mockSchema.getTxResults()).thenReturn(mockMapIndex);

    assertThat(blockchain.getTxResult(messageHash).get()).isEqualTo(txResult);
  }

  @Test
  void getNonexistentTxResult() {
    ProofMapIndexProxy mockMapIndex = mock(ProofMapIndexProxy.class);
    HashCode messageHash = HashCode.fromString("ab");

    when(mockMapIndex.get(messageHash)).thenReturn(null);
    when(mockSchema.getTxResults()).thenReturn(mockMapIndex);

    assertThat(blockchain.getTxResult(messageHash)).isEmpty();
  }

  @Test
  void getTxLocations() {
    MapIndex mockMapIndex = mock(MapIndex.class);
    when(mockSchema.getTxLocations()).thenReturn(mockMapIndex);

    assertThat(blockchain.getTxLocations()).isEqualTo(mockMapIndex);
  }

  @Test
  void getTxLocation() {
    MapIndex mockMapIndex = mock(MapIndex.class);
    HashCode messageHash = HashCode.fromString("ab");
    TransactionLocation txLocation = TransactionLocation.valueOf(1L, 1L);

    when(mockMapIndex.get(messageHash)).thenReturn(txLocation);
    when(mockSchema.getTxLocations()).thenReturn(mockMapIndex);

    assertThat(blockchain.getTxLocation(messageHash).get()).isEqualTo(txLocation);
  }

  @Test
  void getNonexistentTxLocation() {
    MapIndex mockMapIndex = mock(MapIndex.class);
    HashCode messageHash = HashCode.fromString("ab");

    when(mockMapIndex.get(messageHash)).thenReturn(null);
    when(mockSchema.getTxLocations()).thenReturn(mockMapIndex);

    assertThat(blockchain.getTxLocation(messageHash)).isEmpty();
  }

  @Test
  void getBlocks() {
    MapIndex mockMapIndex = mock(MapIndex.class);
    when(mockSchema.getBlocks()).thenReturn(mockMapIndex);

    assertThat(blockchain.getBlocks()).isEqualTo(mockMapIndex);
  }

  @Test
  void getBlock() {
    MapIndex mockMapIndex = mock(MapIndex.class);
    HashCode blockHash = HashCode.fromString("ab");

    when(mockMapIndex.get(blockHash)).thenReturn(BLOCK);
    when(mockSchema.getBlocks()).thenReturn(mockMapIndex);

    assertThat(blockchain.getBlock(blockHash).get()).isEqualTo(BLOCK);
  }

  @Test
  void getNonexistentBlock() {
    MapIndex mockMapIndex = mock(MapIndex.class);
    HashCode blockHash = HashCode.fromString("ab");

    when(mockMapIndex.get(blockHash)).thenReturn(null);
    when(mockSchema.getBlocks()).thenReturn(mockMapIndex);

    assertThat(blockchain.getBlock(blockHash)).isEmpty();
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

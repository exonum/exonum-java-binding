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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.storage.indices.ListIndexProxy;
import com.exonum.binding.storage.indices.ProofListIndexProxy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BlockchainTest {

  private Blockchain blockchain;
  @Mock
  private CoreSchemaProxy mockSchema;

  @Before
  public void setUp() {
    blockchain = new Blockchain(mockSchema);
  }

  @Test
  public void getHeight() {
    long height = 30L;
    when(mockSchema.getHeight()).thenReturn(height);

    assertThat(blockchain.getHeight()).isEqualTo(height);
  }

  @Test
  public void getAllBlockHashes() {
    ListIndexProxy mockListIndex = mock(ListIndexProxy.class);
    when(mockSchema.getAllBlockHashes()).thenReturn(mockListIndex);

    assertThat(blockchain.getAllBlockHashes()).isEqualTo(mockListIndex);
  }

  @Test
  public void getBlockTransactions() {
    ProofListIndexProxy mockListIndex = mock(ProofListIndexProxy.class);
    when(mockSchema.getBlockTransactions(anyLong())).thenReturn(mockListIndex);

    assertThat(blockchain.getBlockTransactions(1L)).isEqualTo(mockListIndex);
  }

}

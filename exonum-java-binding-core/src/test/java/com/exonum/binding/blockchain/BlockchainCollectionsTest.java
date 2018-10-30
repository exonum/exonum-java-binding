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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.storage.indices.ListIndex;
import com.exonum.binding.storage.indices.ListIndexProxy;
import com.exonum.binding.storage.indices.ProofListIndexProxy;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

// TODO: ECR-2592 remove tests ignoring when Unmodifiable collections will be implemented

@RunWith(MockitoJUnitRunner.class)
public class BlockchainCollectionsTest {

  private Blockchain blockchain;
  @Mock
  private CoreSchemaProxy mockSchema;

  @Before
  public void setUp() {
    blockchain = new Blockchain(mockSchema);
  }

  @Ignore
  @Test(expected = UnsupportedOperationException.class)
  public void getAllBlockHashesShouldBeReadOnly() {
    when(mockSchema.getAllBlockHashes()).thenReturn(mock(ListIndexProxy.class));

    ListIndex<HashCode> list = blockchain.getAllBlockHashes();

    list.add(HashCode.fromInt(0x0));
  }

  @Ignore
  @Test(expected = UnsupportedOperationException.class)
  public void getBlockTransactions() {
    when(mockSchema.getBlockTransactions(anyLong())).thenReturn(mock(ProofListIndexProxy.class));

    ProofListIndexProxy<HashCode> list = blockchain.getBlockTransactions(1L);

    list.add(HashCode.fromInt(0x0));
  }

}

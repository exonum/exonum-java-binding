/*
 * Copyright 2020 The Exonum Team
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

package com.exonum.binding.core.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.transaction.RawTransaction;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/*
It is a unit test marked as IT because it loads classes with native methods (which, in turn,
load the native library in static initializers).
 */
@ExtendWith(MockitoExtension.class)
class ServiceNodeProxyIntegrationTest {
  private static final String SERVICE_NAME = "test-service";
  private static final RawTransaction TX = RawTransaction.newBuilder()
      .serviceId(1)
      .transactionId(1)
      .payload(new byte[]{})
      .build();
  private static final Function<BlockchainData, Void> SNAPSHOT_FUNCTION = s -> null;

  @Mock
  private NodeProxy node;
  @Mock
  private BlockchainDataFactory blockchainDataFactory;
  private ServiceNodeProxy decorator;

  @BeforeEach
  void setUp() {
    decorator = new ServiceNodeProxy(node, blockchainDataFactory, SERVICE_NAME);
  }

  @Test
  void submitTransaction() {
    decorator.submitTransaction(TX);

    verify(node).submitTransaction(TX);
  }

  @Test
  void restrictSubmitTransaction() {
    decorator.close();

    assertThrows(IllegalStateException.class, () -> decorator.submitTransaction(TX));
  }

  @Test
  @Disabled("ECR-3828")
  void withSnapshot() {
    decorator.withBlockchainData(SNAPSHOT_FUNCTION);

    verify(node).withSnapshot(any(Function.class));
    verify(blockchainDataFactory).fromRawAccess(any(Snapshot.class), eq(SERVICE_NAME));
  }

  @Test
  void restrictWithSnapshot() {
    decorator.close();

    assertThrows(IllegalStateException.class,
        () -> decorator.withBlockchainData(SNAPSHOT_FUNCTION));
  }

  @Test
  void getPublicKey() {
    PublicKey key = PublicKey.fromHexString("ab");
    when(node.getPublicKey()).thenReturn(key);

    PublicKey actualKey = decorator.getPublicKey();

    assertThat(actualKey).isEqualTo(key);
  }

  @Test
  void restrictGetPublicKey() {
    decorator.close();

    assertThrows(IllegalStateException.class, () -> decorator.getPublicKey());
  }
}

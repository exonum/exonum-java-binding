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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.transaction.RawTransaction;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MultiplexingNodeDecoratorTest {
  private static final RawTransaction TX = RawTransaction.newBuilder()
      .serviceId(1)
      .transactionId(1)
      .payload(new byte[]{})
      .build();
  private static final Function<Snapshot, Void> SNAPSHOT_FUNCTION = s -> null;

  @Mock
  private Node node;
  @InjectMocks
  private MultiplexingNodeDecorator decorator;

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
  void withSnapshot() {
    decorator.withSnapshot(SNAPSHOT_FUNCTION);

    verify(node).withSnapshot(SNAPSHOT_FUNCTION);
  }

  @Test
  void restrictWithSnapshot() {
    decorator.close();

    assertThrows(IllegalStateException.class, () -> decorator.withSnapshot(SNAPSHOT_FUNCTION));
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

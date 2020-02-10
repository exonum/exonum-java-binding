/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.common.blockchain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.messages.core.Blockchain.CallInBlock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CallInBlocksTest {

  @ParameterizedTest
  @ValueSource(ints = {0, 1, Integer.MAX_VALUE - 1})
  void transaction(int txPosition) {
    CallInBlock transaction = CallInBlocks.transaction(txPosition);
    assertThat(transaction.getTransaction()).isEqualTo(txPosition);
  }

  @ParameterizedTest
  @ValueSource(ints = {/* Negative: */ Integer.MIN_VALUE, -2, -1,
      /* Too big: */ Integer.MAX_VALUE})
  void transactionInvalidPositions(int txPosition) {
    assertThrows(IndexOutOfBoundsException.class, () -> CallInBlocks.transaction(txPosition));
  }
}

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

import static com.google.common.base.Preconditions.checkElementIndex;

import com.exonum.core.messages.Blockchain.CallInBlock;

/**
 * Provides factory methods to concisely create {@link
 * com.exonum.core.messages.Blockchain.CallInBlock}s.
 */
public final class CallInBlocks {

  /**
   * Creates a call id corresponding to a transaction method call.
   *
   * @param txPosition a zero-based position of the transaction in the block
   * @throws IndexOutOfBoundsException if position is negative or greater than {@value
   *     Integer#MAX_VALUE}
   */
  public static CallInBlock transaction(int txPosition) {
    checkElementIndex(txPosition, Integer.MAX_VALUE, "txPosition");
    return CallInBlock.newBuilder().setTransaction(txPosition).build();
  }

  /**
   * Creates a call id corresponding to a {@code Service#beforeTransactions} handler call.
   *
   * @param serviceId a numeric identifier of the service which executed 'beforeTransactions'
   */
  public static CallInBlock beforeTransactions(int serviceId) {
    return CallInBlock.newBuilder().setBeforeTransactions(serviceId).build();
  }

  /**
   * Creates a call id corresponding to a {@code Service#afterTransactions} handler call.
   *
   * @param serviceId a numeric identifier of the service which executed 'afterTransactions'
   */
  public static CallInBlock afterTransactions(int serviceId) {
    return CallInBlock.newBuilder().setAfterTransactions(serviceId).build();
  }

  private CallInBlocks() {}
}

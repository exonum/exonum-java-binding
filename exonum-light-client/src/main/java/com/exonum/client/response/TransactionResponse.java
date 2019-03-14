/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.client.response;

import static com.exonum.client.response.TransactionStatus.COMMITTED;
import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.message.TransactionMessage;
import lombok.Value;

@Value
public class TransactionResponse {
  /**
   * Current status of the transaction.
   */
  TransactionStatus status;
  /**
   * Transaction message.
   */
  TransactionMessage message;
  /**
   * Transaction execution result.
   * Not available unless the transaction is {@linkplain #isCommitted committed} to the blockchain.
   */
  TransactionResult executionResult;
  /**
   * Transaction location in the blockchain.
   * Not available unless the transaction is {@linkplain #isCommitted committed} to the blockchain.
   */
  TransactionLocation location;

  /**
   * Returns transaction execution result.
   * @throws IllegalStateException if the transaction is not committed yet
   */
  public TransactionResult getExecutionResult() {
    checkState(status == COMMITTED,
        "Transaction result is available for committed transactions only");
    return executionResult;
  }

  /**
   * Returns transaction location in the blockchain.
   * @throws IllegalStateException if the transaction is not committed yet
   */
  public TransactionLocation getLocation() {
    checkState(status == COMMITTED,
        "Transaction location is available for committed transactions only");
    return location;
  }

  /**
   * Returns {@code true} when the transaction is {@linkplain TransactionStatus#COMMITTED committed}
   * to the blockchain; or {@code false} — otherwise.
   */
  public boolean isCommitted() {
    return status == COMMITTED;
  }

}

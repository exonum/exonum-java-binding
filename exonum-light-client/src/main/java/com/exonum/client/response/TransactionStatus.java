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

import com.exonum.core.messages.Runtime.ExecutionStatus;
import com.google.gson.annotations.SerializedName;

/**
 * Status of a particular transaction.
 */
public enum TransactionStatus {
  /**
   * Shows that transaction is in unconfirmed transaction pool currently.
   */
  @SerializedName("in-pool")
  IN_POOL,
  /**
   * Shows that transaction is committed to the blockchain.
   * Please note that a committed transaction has not necessarily completed
   * successfully — use the {@linkplain ExecutionStatus execution result}
   * to check that.
   */
  @SerializedName("committed")
  COMMITTED
}

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
 */

package com.exonum.binding.blockchain;

import com.google.auto.value.AutoValue;

/**
 * Transaction position in the blockchain. Enumeration begins from 0.
 */
@AutoValue
public abstract class TransactionLocation {

  public static TransactionLocation valueOf(long height, long indexInBlock) {
    return new AutoValue_TransactionLocation(height, indexInBlock);
  }

  /**
   * Height of the block where the transaction was committed.
   */
  public abstract long getHeight();

  /**
   * Zero-based position of this transaction in the block. Transactions executed in the ascending
   * order of these indices.
   */
  public abstract long getIndexInBlock();

}

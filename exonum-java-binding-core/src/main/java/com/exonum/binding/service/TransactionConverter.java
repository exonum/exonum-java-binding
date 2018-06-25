/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.service;

import com.exonum.binding.annotations.AutoGenerationCandidate;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Transaction;

/**
 * A converter of a binary Exonum message, which contains transaction data,
 * into an executable transaction.
 */
@FunctionalInterface
@AutoGenerationCandidate(reason = "Perfectly viable given a service id "
    + "and description of all transactions (ids & implementing classes")
public interface TransactionConverter {

  /**
   * Converts an Exonum transaction message to an executable transaction of some service.
   *
   * @param message a transaction message (i.e., whose message type is a transaction)
   * @return an executable transaction of some service
   * @throws IllegalArgumentException if the message is not a transaction,
   *                                  or a transaction of an unknown service
   * @throws NullPointerException if message is null
   */
  Transaction toTransaction(BinaryMessage message);
}

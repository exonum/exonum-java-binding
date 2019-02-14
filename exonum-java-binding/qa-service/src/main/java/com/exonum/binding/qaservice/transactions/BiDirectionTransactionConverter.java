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

package com.exonum.binding.qaservice.transactions;

import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;

/**
 * A converter between executable transaction and Exonum binary raw transaction data.
 *
 * @param <TransactionT> a type of transaction. You can have a single
 *     {@link BiDirectionTransactionConverter} that works with any transactions of this service
 *     or a converter for each transaction type.
 */
public interface BiDirectionTransactionConverter<TransactionT extends Transaction> {

  /**
   * Converts a raw transaction into an executable transaction.
   */
  TransactionT fromRawTransaction(RawTransaction rawTransaction);

  /**
   * Returns a raw transaction, representing the given transaction.
   *
   * <p>In a more general case, one will have to provide the signature, network id, etc.,
   * see class level comment.
   */
  RawTransaction toRawTransaction(TransactionT transaction);
}

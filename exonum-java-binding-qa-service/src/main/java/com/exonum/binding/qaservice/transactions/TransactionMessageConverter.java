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

import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.qaservice.PromoteToCore;
import com.exonum.binding.transaction.Transaction;

/**
 * A converter between executable transaction & its binary message.
 *
 * @param <TransactionT> a type of transaction. You can have a single
 *     {@link TransactionMessageConverter} that works with any transactions of this service
 *     or a converter for each transaction type.
 */
@PromoteToCore("… with first-class support of transactions that don’t need a message to operate."
    + "Consider splitting into two functional interfaces, so that some APIs may use "
    + "method references."
    + "An open question is how you convert transaction into message if not all data "
    + "is known (and must be known by transaction): a network id, a protocol version, "
    + "the signature. These things do not comprise the transaction parameters."
)
interface TransactionMessageConverter<TransactionT extends Transaction> {

  /**
   * Converts a message into an executable transaction.
   */
  TransactionT fromMessage(Message txMessage);

  /**
   * Returns a message, representing the given transaction.
   *
   * <p>In a more general case, one will have to provide the signature, network id, etc.,
   * see class level comment.
   */
  BinaryMessage toMessage(TransactionT transaction);
}

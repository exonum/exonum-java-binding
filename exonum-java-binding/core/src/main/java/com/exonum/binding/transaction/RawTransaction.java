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

package com.exonum.binding.transaction;

import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.service.Service;
import com.exonum.binding.service.TransactionConverter;
import com.google.auto.value.AutoValue;

/**
 * An Exonum raw transaction. The raw transaction is different from {@link TransactionMessage}
 * as it only includes the serialized transaction parameters and transaction identifiers.
 * Author’s public key is <em>not</em> included but is accessible from
 * the {@linkplain TransactionContext#getAuthorPk() execution context}.
 *
 * <p>A raw transaction is converted to an {@linkplain Transaction executable transaction}
 * by the framework using an implementation of {@link TransactionConverter}.
 */
@AutoValue
public abstract class RawTransaction {

  /**
   * Returns a service identifier which the transaction belongs to.
   * @see Service#getId()
   */
  public abstract short getServiceId();

  /**
   * Returns the type of this transaction within a service. Unique within the service.
   * @see TransactionMessage#getTransactionId
   */
  public abstract short getTransactionId();

  /**
   * Returns the transaction payload which contains actual transaction data.
   */
  public abstract byte[] getPayload();

  /**
   * Returns the new builder for the transaction.
   */
  public static RawTransaction.Builder newBuilder() {
    return new AutoValue_RawTransaction.Builder();
  }

  /**
   * Creates a raw transaction from the given transaction message.
   * The returned transaction will have the same service id, transaction id, and payload
   * as the given message.
   */
  public static RawTransaction fromMessage(TransactionMessage txMessage) {
    return newBuilder()
        .serviceId(txMessage.getServiceId())
        .transactionId(txMessage.getTransactionId())
        .payload(txMessage.getPayload())
        .build();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    /**
     * Sets the identifier of the service this transaction belongs to.
     */
    public abstract Builder serviceId(short serviceId);

    /**
     * Sets the identifier of the transaction within a service.
     */
    public abstract Builder transactionId(short transactionId);

    /**
     * Sets the payload of the transaction.
     */
    public abstract Builder payload(byte[] payload);

    /**
     * Creates the RawTransaction instance with specified parameters.
     */
    public abstract RawTransaction build();
  }

}

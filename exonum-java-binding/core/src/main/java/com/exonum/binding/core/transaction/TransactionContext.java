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

package com.exonum.binding.core.transaction;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.storage.database.Fork;

/**
 * Transaction context class. Contains required information for the transaction execution.
 * The context is provided by the framework and users shouldn't create context instances manually
 * except tests.
 */
public interface TransactionContext {
  /**
   * Returns database view allowing R/W operations.
   */
  Fork getFork();

  /**
   * Returns SHA-256 hash of the {@linkplain TransactionMessage transaction message} that
   * carried the payload of the transaction.
   * Each transaction message is uniquely identified by its hash; the messages are persisted
   * in the {@linkplain Blockchain#getTxMessages() blockchain} and can be fetched by this hash.
   */
  HashCode getTransactionMessageHash();

  /**
   * Returns public key of the transaction author. The corresponding transaction message
   * is guaranteed to have a correct {@link CryptoFunctions#ed25519()} signature
   * with this public key.
   */
  PublicKey getAuthorPk();

  /**
   * Returns the name of the service instance.
   *
   * @see ServiceInstanceSpec#getName()
   */
  String getServiceName();

  /**
   * Returns the numeric id of the service instance.
   *
   * @see ServiceInstanceSpec#getId()
   */
  int getServiceId();

  /**
   * Returns the builder of the transaction context.
   */
  static Builder builder() {
    return new Builder();
  }

  /**
   * Transaction context builder.
   */
  final class Builder {
    private Fork fork;
    private HashCode hash;
    private PublicKey authorPk;
    private String serviceName;
    private Integer serviceId;

    /**
     * Sets database fork for the context.
     */
    public Builder fork(Fork fork) {
      this.fork = fork;
      return this;
    }

    /**
     * Sets transaction message hash for the context.
     */
    public Builder txMessageHash(HashCode hash) {
      this.hash = hash;
      return this;
    }

    /**
     * Sets transaction author public key for the context.
     */
    public Builder authorPk(PublicKey authorPk) {
      this.authorPk = authorPk;
      return this;
    }

    /**
     * Sets service name for the context.
     */
    public Builder serviceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    /**
     * Sets service id for the context.
     */
    public Builder serviceId(int serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    /**
     * Creates the transaction context instance.
     */
    public TransactionContext build() {
      return InternalTransactionContext.newInstance(fork, hash, authorPk, serviceName,
          checkNotNull(serviceId));
    }

    private Builder() {}
  }
}

/*
 * Copyright 2020 The Exonum Team
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

package com.exonum.binding.core.service;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.storage.database.Prefixed;
import com.exonum.binding.core.transaction.Transaction;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * An execution context. The context provides access to the blockchain data
 * for the executing service, and also contains the required information for the transaction
 * execution.
 *
 * <p>The context is provided by the framework and users shouldn't create context instances manually
 * except in tests.
 *
 * @see Transaction
 */
public interface ExecutionContext {

  /**
   * Returns the prefixed database access for the executing service. Allows R/W operations.
   *
   * <p>A shortcut for {@code context.getBlockchainData().getExecutingServiceData()}.
   *
   * @see #getBlockchainData()
   */
  default Prefixed getServiceData() {
    return getBlockchainData().getExecutingServiceData();
  }

  /**
   * Returns the database access object allowing R/W operations.
   *
   * @see #getServiceData()
   */
  BlockchainData getBlockchainData();

  /**
   * Returns SHA-256 hash of the {@linkplain TransactionMessage transaction message} that
   * carried the payload of the transaction; or {@code Optional.empty()} if no message corresponds
   * to this context.
   *
   * <p>Each transaction message is uniquely identified by its hash; the messages are persisted
   * in the {@linkplain Blockchain#getTxMessages() blockchain} and can be fetched by this hash.
   */
  Optional<HashCode> getTransactionMessageHash();

  /**
   * Returns public key of the transaction author; or {@code Optional.empty()} if no transaction
   * message corresponds to this context.
   *
   * <p>The corresponding transaction message, if any, is guaranteed to have a correct
   * {@link CryptoFunctions#ed25519()} signature with this public key.
   */
  Optional<PublicKey> getAuthorPk();

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
    private BlockchainData blockchainData;
    private HashCode hash;
    private PublicKey authorPk;
    private String serviceName;
    private Integer serviceId;

    /**
     * Sets the blockchain data for the context.
     */
    public Builder blockchainData(BlockchainData blockchainData) {
      this.blockchainData = blockchainData;
      return this;
    }

    /**
     * Sets transaction message hash for the context.
     */
    public Builder txMessageHash(@Nullable HashCode hash) {
      this.hash = hash;
      return this;
    }

    /**
     * Sets transaction author public key for the context.
     */
    public Builder authorPk(@Nullable PublicKey authorPk) {
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
    public ExecutionContext build() {
      return InternalExecutionContext.newInstance(blockchainData, hash, authorPk, serviceName,
          checkNotNull(serviceId));
    }

    private Builder() {}
  }
}

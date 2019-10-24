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

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.storage.database.Fork;
import java.util.Objects;

/**
 * Default implementation of the transaction context.
 */
final class InternalTransactionContext implements TransactionContext {
  private final Fork fork;
  private final HashCode hash;
  private final PublicKey authorPk;
  private final String serviceName;
  private final int serviceId;

  InternalTransactionContext(Fork fork, HashCode hash, PublicKey authorPk, String serviceName,
                             Integer serviceId) {
    this.fork = checkNotNull(fork);
    this.hash = checkNotNull(hash);
    this.authorPk = checkNotNull(authorPk);
    this.serviceName = checkNotNull(serviceName);
    this.serviceId = checkNotNull(serviceId);
  }

  @Override
  public Fork getFork() {
    return fork;
  }

  @Override
  public HashCode getTransactionMessageHash() {
    return hash;
  }

  @Override
  public PublicKey getAuthorPk() {
    return authorPk;
  }

  @Override
  public String getServiceName() {
    return serviceName;
  }

  @Override
  public Integer getServiceId() {
    return serviceId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InternalTransactionContext that = (InternalTransactionContext) o;
    return serviceId == that.serviceId
        && Objects.equals(fork, that.fork)
        && Objects.equals(hash, that.hash)
        && Objects.equals(authorPk, that.authorPk)
        && Objects.equals(serviceName, that.serviceName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fork, hash, authorPk, serviceName, serviceId);
  }
}

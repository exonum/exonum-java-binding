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

package com.exonum.binding.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
import java.util.List;

/**
 * A base class for user services.
 */
public abstract class AbstractService implements Service {

  private final short id;
  private final String name;
  private final TransactionConverter transactionConverter;

  /**
   * Creates an AbstractService.
   *
   * @param id an id of the service
   * @param name a name of the service
   * @param transactionConverter a transaction converter that is aware of
   *                             all transactions of this service
   */
  protected AbstractService(short id, String name, TransactionConverter transactionConverter) {
    this.id = id;
    checkArgument(!name.isEmpty(), "The service name must not be empty");
    this.name = name;
    this.transactionConverter = checkNotNull(transactionConverter);
  }

  @Override
  public short getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Transaction convertToTransaction(RawTransaction message) {
    return transactionConverter.toTransaction(message);
  }

  @Override
  public List<HashCode> getStateHashes(Snapshot snapshot) {
    return createDataSchema(snapshot).getStateHashes();
  }

  /**
   * Creates a data schema of this service.
   *
   * @param view a database view
   * @return a data schema of the service
   */
  protected abstract Schema createDataSchema(View view);
}

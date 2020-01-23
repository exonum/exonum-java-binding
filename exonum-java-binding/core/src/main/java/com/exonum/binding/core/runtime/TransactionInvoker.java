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

package com.exonum.binding.core.runtime;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.core.service.Service;
import com.exonum.binding.core.transaction.ExecutionException;
import com.exonum.binding.core.transaction.TransactionContext;
import com.google.inject.Inject;
import java.util.Map;

/** Stores ids of transaction methods and their method handles of a corresponding service. */
final class TransactionInvoker {
  private final Service service;
  private final Map<Integer, TransactionMethod> transactionMethods;

  @Inject
  TransactionInvoker(Service service) {
    this.service = service;
    this.transactionMethods = TransactionExtractor.extractTransactionMethods(service.getClass());
  }

  /**
   * Invoke the transaction method with a given transaction identifier.
   *
   * @param transactionId a transaction method identifier
   * @param context a transaction execution context
   * @param arguments the serialized transaction arguments
   * @throws IllegalArgumentException if there is no transaction method with given id in a
   *     corresponding service
   * @throws ExecutionException if {@link ExecutionException} was thrown by the transaction method,
   *     it is propagated
   * @throws UnexpectedExecutionException if any other exception is thrown by the transaction
   *     method, it is wrapped as cause
   */
  void invokeTransaction(int transactionId, byte[] arguments, TransactionContext context) {
    checkArgument(
        transactionMethods.containsKey(transactionId),
        "No method with transaction id (%s)",
        transactionId);
    TransactionMethod transactionMethod = transactionMethods.get(transactionId);
    transactionMethod.invoke(service, arguments, context);
  }
}

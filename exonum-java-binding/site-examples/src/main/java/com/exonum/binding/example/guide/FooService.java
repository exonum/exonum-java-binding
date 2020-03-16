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

package com.exonum.binding.example.guide;

import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.service.AbstractService;
import com.exonum.binding.core.service.ExecutionContext;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.transaction.Transaction;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;

@SuppressWarnings("unused") // Example code
public final class FooService extends AbstractService {

  // ci-block ci_put_tx {
  /** A numeric identifier of the "put" transaction. */
  private static final int PUT_TX_ID = 0;
  // }

  @Inject
  public FooService(ServiceInstanceSpec instanceSpec) {
    super(instanceSpec);
  }

  // ci-block ci_put_tx {
  /**
   * Puts an entry (a key-value pair) into the test proof map.
   *
   * <p>{@code Transactions.PutTransactionArgs} is a Protoc-generated
   * class with the transaction method arguments.
   */
  @Transaction(PUT_TX_ID)
  public void putEntry(Transactions.PutEntryArgs arguments,
      ExecutionContext context) {
    var schema = new FooSchema(context.getServiceData());
    String key = arguments.getKey();
    String value = arguments.getValue();
    schema.testMap()
        .put(key, value);
  }
  // }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    // No handlers
  }
}

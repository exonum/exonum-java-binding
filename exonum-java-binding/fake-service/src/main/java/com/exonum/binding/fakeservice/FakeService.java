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

package com.exonum.binding.fakeservice;

import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.service.AbstractService;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.transaction.ExecutionContext;
import com.exonum.binding.core.transaction.ExecutionException;
import com.exonum.binding.core.transaction.Transaction;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;

public final class FakeService extends AbstractService {

  public static final int PUT_TX_ID = 0;
  public static final int RAISE_ERROR_TX_ID = 1;

  @Inject
  FakeService(ServiceInstanceSpec instanceSpec) {
    super(instanceSpec);
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    // No handlers
  }

  /**
   * Puts an entry (a key-value pair) into the test proof map.
   */
  @Transaction(PUT_TX_ID)
  public void putEntry(Transactions.PutTransactionArgs arguments,
      ExecutionContext context) {
    FakeSchema schema = new FakeSchema(context.getServiceData());
    String key = arguments.getKey();
    String value = arguments.getValue();
    schema.testMap()
        .put(key, value);
  }

  /**
   * Throws an exception with the given error code and description.
   */
  @Transaction(RAISE_ERROR_TX_ID)
  public void raiseError(Transactions.RaiseErrorArgs arguments, ExecutionContext context) {
    throw new ExecutionException((byte) arguments.getCode());
  }
}

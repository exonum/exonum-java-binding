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

package com.exonum.binding.testkit;

import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.service.AbstractService;
import com.exonum.binding.core.service.BlockCommittedEvent;
import com.exonum.binding.core.service.Configuration;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.storage.database.Prefixed;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.core.transaction.ExecutionException;
import com.exonum.binding.core.transaction.RawTransaction;
import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.core.transaction.ExecutionContext;
import com.exonum.binding.testkit.TestProtoMessages.TestConfiguration;
import com.exonum.binding.testkit.Transactions.PutTransactionArgs;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;

public final class TestService extends AbstractService {

  static final String INITIAL_ENTRY_KEY = "Initial key";
  static final String THROWING_VALUE = "Incorrect value";
  static final byte ANY_ERROR_CODE = 127;
  static final short TEST_TRANSACTION_ID = 94;

  private Node node;

  @Inject
  public TestService(ServiceInstanceSpec serviceSpec) {
    super(serviceSpec);
  }

  @Override
  public void initialize(ExecutionContext context, Configuration configuration) {
    TestConfiguration initialConfiguration = configuration.getAsMessage(TestConfiguration.class);
    String configurationValue = initialConfiguration.getValue();
    if (configurationValue.equals(THROWING_VALUE)) {
      throw new ExecutionException(ANY_ERROR_CODE, "Service configuration had an invalid value: "
          + configurationValue);
    }
    TestSchema schema = new TestSchema(context.getServiceData());
    ProofMapIndexProxy<String, String> testMap = schema.testMap();
    testMap.put(INITIAL_ENTRY_KEY, configurationValue);
  }

  @Transaction(TEST_TRANSACTION_ID)
  public void putEntry(PutTransactionArgs arguments, ExecutionContext context) {
    Prefixed serviceData = context.getServiceData();
    TestSchema schema = new TestSchema(serviceData);

    String key = arguments.getKey();
    String value = arguments.getValue();
    schema.testMap()
        .put(key, value);
  }

  @Override
  public void afterCommit(BlockCommittedEvent event) {
    long height = event.getHeight();
    RawTransaction rawTransaction = constructAfterCommitTransaction(getId(), height);
    node.submitTransaction(rawTransaction);
  }

  static RawTransaction constructAfterCommitTransaction(int serviceId, long height) {
    String key = String.valueOf(height);
    String value = "Test message on height " + height;
    return RawTransaction.newBuilder()
        .serviceId(serviceId)
        .transactionId(TEST_TRANSACTION_ID)
        .payload(
            PutTransactionArgs.newBuilder()
                .setKey(key)
                .setValue(value)
                .build()
                .toByteArray())
        .build();
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    this.node = node;
  }
}

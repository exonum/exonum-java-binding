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

import static com.exonum.binding.testkit.TestTransaction.BODY_CHARSET;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.service.AbstractService;
import com.exonum.binding.core.service.BlockCommittedEvent;
import com.exonum.binding.core.service.Configuration;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.core.transaction.RawTransaction;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import java.nio.charset.StandardCharsets;

final class TestService extends AbstractService {

  static final HashCode INITIAL_ENTRY_KEY = Hashing.defaultHashFunction()
      .hashString("initial key", StandardCharsets.UTF_8);
  static final String INITIAL_ENTRY_VALUE = "initial value";

  private final int serviceInstanceId;
  private Node node;

  @Inject
  public TestService(ServiceInstanceSpec serviceSpec) {
    this.serviceInstanceId = serviceSpec.getId();
  }

  Node getNode() {
    return node;
  }

  @Override
  protected TestSchema createDataSchema(View view) {
    return new TestSchema(view, serviceInstanceId);
  }

  @Override
  public void initialize(Fork fork, Configuration configuration) {
    TestConfiguration initialConfiguration = configuration.getAsMessage(TestConfiguration.class);
    String configurationValue = initialConfiguration.getValue();
    TestSchema schema = createDataSchema(fork);
    ProofMapIndexProxy<HashCode, String> testMap = schema.testMap();
    testMap.put(INITIAL_ENTRY_KEY, configurationValue);
  }

  @Override
  public void afterCommit(BlockCommittedEvent event) {
    long height = event.getHeight();
    RawTransaction rawTransaction = constructAfterCommitTransaction(serviceInstanceId, height);
    node.submitTransaction(rawTransaction);
  }

  static RawTransaction constructAfterCommitTransaction(int serviceId, long height) {
    String payload = "Test message on height " + height;
    return RawTransaction.newBuilder()
        .serviceId(serviceId)
        .transactionId(TestTransaction.ID)
        .payload(payload.getBytes(BODY_CHARSET))
        .build();
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    this.node = node;
  }
}

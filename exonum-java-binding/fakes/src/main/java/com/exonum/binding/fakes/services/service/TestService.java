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

package com.exonum.binding.fakes.services.service;

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
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A test service for integration tests.
 *
 * <p>Used in native code, see exonum-java-binding/core/rust/integration_tests.
 */
@SuppressWarnings("WeakerAccess")
public final class TestService extends AbstractService {

  public static final short ID = 0x110B;
  public static final String NAME = "experimentalTestService";

  static final HashCode INITIAL_ENTRY_KEY = Hashing.defaultHashFunction()
      .hashString("initial key", StandardCharsets.UTF_8);
  static final String INITIAL_ENTRY_VALUE = "initial value";
  static final HashCode BEFORE_COMMIT_ENTRY_KEY = Hashing.defaultHashFunction()
          .hashString("bc key", StandardCharsets.UTF_8);
  static final Integer BEFORE_COMMIT_INITIAL_VALUE = Integer.valueOf(0);

  private static final Logger logger = LogManager.getLogger(TestService.class);

  private static final SchemaFactory<TestSchema> SCHEMA_FACTORY = TestSchema::new;

  @Inject
  public TestService(ServiceInstanceSpec instanceSpec) {
    super(instanceSpec);
  }

  @Override
  protected TestSchema createDataSchema(View view) {
    return SCHEMA_FACTORY.from(view);
  }

  /**
   * Always puts the same value identified by the same key.
   */
  @Override
  public void initialize(Fork fork, Configuration configuration) {
    TestSchema schema = createDataSchema(fork);
    ProofMapIndexProxy<HashCode, String> testMap = schema.initializeServiceMap();
    testMap.put(INITIAL_ENTRY_KEY, INITIAL_ENTRY_VALUE);
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    // No-op: no handlers.
  }

  @Override
  public void beforeCommit(Fork fork) {
    try {
      TestSchema schema = createDataSchema(fork);
      ProofMapIndexProxy<HashCode, String> map = schema.beforeCommitMap();
      String value = map.get(BEFORE_COMMIT_ENTRY_KEY);
      Integer newValue = (value != null)
          ? Integer.valueOf(value) + 1
          : BEFORE_COMMIT_INITIAL_VALUE;
      map.put(BEFORE_COMMIT_ENTRY_KEY, newValue.toString());
    } catch (Throwable th) {
      String message = "Couldn't execute beforeCommit for service " + TestService.class.getName();
      logger.error(message, th);
    }
  }

  @Override
  public void afterCommit(BlockCommittedEvent event) {

  }
}

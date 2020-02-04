/*
 * Copyright 2020 The Exonum Team
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

package com.exonum.binding.test.service;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.service.AbstractService;
import com.exonum.binding.core.service.Configuration;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.storage.database.Access;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class TestService extends AbstractService {

  @Inject
  protected TestService(ServiceInstanceSpec instanceSpec) {
    super(instanceSpec);
  }

  @Override
  protected TestSchema createDataSchema(Access access) {
    return new TestSchema(access, getId());
  }

  @Override
  public void initialize(Fork fork, Configuration configuration) {
    Properties config = configuration.getAsProperties();

    TestSchema schema = createDataSchema(fork);
    ProofMapIndexProxy<HashCode, String> testMap = schema.testMap();
    config.forEach((k, v) -> testMap.put(toMapKey((String) k), (String) v));
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {

  }

  private static HashCode toMapKey(String key) {
    return Hashing.defaultHashFunction().hashString(key, StandardCharsets.UTF_8);
  }
}

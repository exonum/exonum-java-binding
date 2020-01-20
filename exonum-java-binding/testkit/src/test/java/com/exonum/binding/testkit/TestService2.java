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
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.storage.database.AbstractAccess;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;

public final class TestService2 extends AbstractService {

  private final int serviceInstanceId;

  @Inject
  public TestService2(ServiceInstanceSpec serviceSpec) {
    super(serviceSpec);
    this.serviceInstanceId = serviceSpec.getId();
  }

  @Override
  protected TestSchema createDataSchema(AbstractAccess access) {
    return new TestSchema(access, serviceInstanceId);
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    // No handlers
  }
}

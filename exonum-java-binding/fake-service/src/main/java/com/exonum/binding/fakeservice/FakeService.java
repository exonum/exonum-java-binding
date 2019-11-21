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
import com.exonum.binding.core.storage.database.View;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;

final class FakeService extends AbstractService {

  @Inject
  FakeService(ServiceInstanceSpec instanceSpec) {
    super(instanceSpec);
  }

  @Override
  protected FakeSchema createDataSchema(View view) {
    String name = getName();
    return new FakeSchema(name, view);
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    // No handlers
  }
}

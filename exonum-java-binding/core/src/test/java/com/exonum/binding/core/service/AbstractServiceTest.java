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

package com.exonum.binding.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.exonum.binding.core.runtime.ServiceArtifactId;
import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.storage.database.Access;
import io.vertx.ext.web.Router;
import org.junit.jupiter.api.Test;

class AbstractServiceTest {

  private static final String NAME = "test";
  private static final int ID = 1;
  private static final ServiceInstanceSpec INSTANCE_SPEC = ServiceInstanceSpec.newInstance(NAME, ID,
      ServiceArtifactId.newJavaId("g/a","1"));

  @Test
  void getName() {
    AbstractService service = new ServiceUnderTest(INSTANCE_SPEC);
    assertThat(service.getName()).isEqualTo(NAME);
  }

  @Test
  void getId() {
    AbstractService service = new ServiceUnderTest(INSTANCE_SPEC);
    assertThat(service.getId()).isEqualTo(ID);
  }

  static class ServiceUnderTest extends AbstractService {

    ServiceUnderTest(ServiceInstanceSpec instanceSpec) {
      super(instanceSpec);
    }

    @Override
    protected Schema createDataSchema(Access access) {
      return mock(Schema.class);
    }

    @Override
    public void createPublicApiHandlers(Node node, Router router) {
    }
  }
}

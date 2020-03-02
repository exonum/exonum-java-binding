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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.exonum.binding.common.runtime.ServiceArtifactId;
import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.service.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;

class FakeServiceModuleTest {

  @Test
  void configure() {
    // todo: Do we need this wonderful test?
    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(ServiceInstanceSpec.class).toInstance(ServiceInstanceSpec.newInstance("test", 1,
            ServiceArtifactId.newJavaId("a/b", "1")));
      }
    }, new FakeServiceModule());

    Service instance = injector.getInstance(Service.class);

    assertNotNull(instance);
    assertThat(instance, instanceOf(FakeService.class));
  }
}

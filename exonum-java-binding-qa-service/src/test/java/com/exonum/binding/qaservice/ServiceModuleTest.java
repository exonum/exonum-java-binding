/*
 * Copyright 2018 The Exonum Team
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

package com.exonum.binding.qaservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.service.Service;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Test;

public class ServiceModuleTest {

  @Test
  public void testServiceBindingsSufficient() {
    Injector injector = createInjector();

    Service s = injector.getInstance(Service.class);

    assertThat(s).isInstanceOf(QaServiceImpl.class);
  }

  @Test
  public void testServiceIsSingleton() {
    Injector injector = createInjector();

    Service service1 = injector.getInstance(Service.class);
    Service service2 = injector.getInstance(Service.class);

    assertThat(service1).isSameAs(service2);

    QaService qaService = injector.getInstance(QaService.class);
    assertThat(service1).isSameAs(qaService);
  }

  private Injector createInjector() {
    return Guice.createInjector(new ServiceModule());
  }
}

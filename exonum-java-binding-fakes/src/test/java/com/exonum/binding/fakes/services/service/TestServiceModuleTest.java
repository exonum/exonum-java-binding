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

package com.exonum.binding.fakes.services.service;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import com.exonum.binding.service.Service;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Test;

public class TestServiceModuleTest {

  @Test
  public void configure() {
    Injector injector = Guice.createInjector(new TestServiceModule());

    Service instance = injector.getInstance(Service.class);

    assertNotNull(instance);
    assertThat(instance, instanceOf(TestService.class));
  }
}

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

package com.exonum.binding.runtime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.runtime.ReflectiveModuleSupplierTest.Modules.BadInaccessibleCtor;
import com.exonum.binding.runtime.ReflectiveModuleSupplierTest.Modules.BadNoNoArgCtor;
import com.exonum.binding.runtime.ReflectiveModuleSupplierTest.Modules.BadThrowsInCtor;
import com.exonum.binding.runtime.ReflectiveModuleSupplierTest.Modules.Good;
import com.exonum.binding.service.AbstractServiceModule;
import com.exonum.binding.service.ServiceModule;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"WeakerAccess", "unused"})
class ReflectiveModuleSupplierTest {

  ReflectiveModuleSupplier supplier;

  @Test
  void newFailsIfNoConstuctor() {
    assertThrows(NoSuchMethodException.class,
        () -> new ReflectiveModuleSupplier(BadNoNoArgCtor.class));
  }

  @Test
  void newFailsIfInaccessibleConstuctor() {
    assertThrows(IllegalAccessException.class,
        () -> new ReflectiveModuleSupplier(BadInaccessibleCtor.class));
  }

  @Test
  void get() throws NoSuchMethodException, IllegalAccessException {
    supplier = new ReflectiveModuleSupplier(Good.class);
    ServiceModule serviceModule = supplier.get();
    assertThat(serviceModule, instanceOf(Good.class));
  }

  @Test
  void getProducesFreshInstances() throws NoSuchMethodException, IllegalAccessException {
    supplier = new ReflectiveModuleSupplier(Good.class);
    ServiceModule serviceModule1 = supplier.get();
    ServiceModule serviceModule2 = supplier.get();
    // Check *both* are OK
    assertThat(serviceModule1, instanceOf(Good.class));
    assertThat(serviceModule2, instanceOf(Good.class));
    // Check they are not the same
    assertThat(serviceModule1, not(sameInstance(serviceModule2)));
  }

  @Test
  void getPropagatesExceptions() throws NoSuchMethodException, IllegalAccessException {
    supplier = new ReflectiveModuleSupplier(BadThrowsInCtor.class);

    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> supplier.get());

    Throwable cause = e.getCause();
    assertThat(cause, instanceOf(RuntimeException.class));
    assertThat(cause.getMessage(), equalTo("BadThrowsInCtor indeed"));
  }

  static class Modules {
    static class BadNoNoArgCtor extends AbstractServiceModule {
      BadNoNoArgCtor(String s1) {
      }
    }

    static class BadInaccessibleCtor extends AbstractServiceModule {
      private BadInaccessibleCtor() {
      }
    }

    static class BadThrowsInCtor extends AbstractServiceModule {
      BadThrowsInCtor() {
        throw new RuntimeException("BadThrowsInCtor indeed");
      }
    }

    static class Good extends AbstractServiceModule {
    }
  }
}

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.service.AbstractServiceModule;
import com.exonum.binding.service.Node;
import com.exonum.binding.service.Service;
import com.exonum.binding.service.ServiceModule;
import com.exonum.binding.service.TransactionConverter;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
import com.google.inject.Singleton;
import io.vertx.ext.web.Router;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

class TestKitTest {

  @Test
  void createTestKitForSingleService() {
    TestKit testKit = TestKit.forService(TestServiceModule.class);
    Service service = testKit.getService(TestService.SERVICE_ID, TestService.class);
    assertEquals(service.getId(), TestService.SERVICE_ID);
    assertEquals(service.getName(), TestService.SERVICE_NAME);
  }

  @Test
  void createTestKitWithBuilderForSingleService() {
    TestKit testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withService(TestServiceModule.class)
        .build();
    Service service = testKit.getService(TestService.SERVICE_ID, TestService.class);
    assertEquals(service.getId(), TestService.SERVICE_ID);
    assertEquals(service.getName(), TestService.SERVICE_NAME);
  }

  @Test
  void createTestKitWithBuilderForMultipleSameServices() {
    List<Class<? extends ServiceModule>> serviceModules = new ArrayList<>();
    serviceModules.add(TestServiceModule.class);
    serviceModules.add(TestServiceModule.class);
    TestKit testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withServices(serviceModules)
        .build();
    Service service = testKit.getService(TestService.SERVICE_ID, TestService.class);
    assertEquals(service.getId(), TestService.SERVICE_ID);
    assertEquals(service.getName(), TestService.SERVICE_NAME);
  }

  @Test
  void createTestKitWithBuilderForMultipleDifferentServices() {
    TestKit testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withService(TestServiceModule.class)
        .withService(TestServiceModule2.class)
        .build();
    Service service = testKit.getService(TestService.SERVICE_ID, TestService.class);
    Service service2 = testKit.getService(TestService2.SERVICE_ID, TestService2.class);
    assertEquals(service.getId(), TestService.SERVICE_ID);
    assertEquals(service.getName(), TestService.SERVICE_NAME);
    assertEquals(service2.getId(), TestService2.SERVICE_ID);
    assertEquals(service2.getName(), TestService2.SERVICE_NAME);
  }

  @Test
  void requestWrongServiceClass() {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    TestKit testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withService(TestServiceModule.class)
        .build();
    assertThrows(exceptionType, () -> testKit.getService(TestService.SERVICE_ID, TestService2.class));
  }

  @Test
  void createTestKitMoreThanMaxServiceNumber() {
    Class<IllegalArgumentException> exceptionType = IllegalArgumentException.class;
    List<Class<? extends ServiceModule>> serviceModules = new ArrayList<>();
    for (int i = 0; i < TestKit.MAX_SERVICE_NUMBER + 1; i++) {
      serviceModules.add(TestServiceModule.class);
    }
    TestKit.Builder testKitBuilder = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withServices(serviceModules);
    assertThrows(exceptionType, testKitBuilder::build);
  }

  static final class TestServiceModule extends AbstractServiceModule {
    private static final TransactionConverter THROWING_TX_CONVERTER = (tx) -> {
      throw new IllegalStateException("No transactions in this service: " + tx);
    };

    @Override
    protected void configure() {
      bind(Service.class).to(TestService.class).in(Singleton.class);
      bind(TransactionConverter.class).toInstance(THROWING_TX_CONVERTER);
    }
  }

  static final class TestServiceModule2 extends AbstractServiceModule {
    private static final TransactionConverter THROWING_TX_CONVERTER = (tx) -> {
      throw new IllegalStateException("No transactions in this service: " + tx);
    };

    @Override
    protected void configure() {
      bind(Service.class).to(TestService2.class).in(Singleton.class);
      bind(TransactionConverter.class).toInstance(THROWING_TX_CONVERTER);
    }
  }

  static final class TestService implements Service {

    static short SERVICE_ID = 42;
    static String SERVICE_NAME = "Test service";

    @Override
    public short getId() {
      return SERVICE_ID;
    }

    @Override
    public String getName() {
      return SERVICE_NAME;
    }

    @Override
    public Transaction convertToTransaction(RawTransaction rawTransaction) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void createPublicApiHandlers(Node node, Router router) {
      throw new UnsupportedOperationException();
    }
  }

  static final class TestService2 implements Service {

    static short SERVICE_ID = 48;
    static String SERVICE_NAME = "Test service 2";

    @Override
    public short getId() {
      return SERVICE_ID;
    }

    @Override
    public String getName() {
      return SERVICE_NAME;
    }

    @Override
    public Transaction convertToTransaction(RawTransaction rawTransaction) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void createPublicApiHandlers(Node node, Router router) {
      throw new UnsupportedOperationException();
    }
  }
}

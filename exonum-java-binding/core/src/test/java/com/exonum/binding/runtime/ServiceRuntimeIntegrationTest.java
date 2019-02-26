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

import static com.exonum.binding.runtime.TestService.ID;
import static com.exonum.binding.runtime.TestService.NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.exonum.binding.service.adapters.UserServiceAdapter;
import com.exonum.binding.service.adapters.ViewFactory;
import com.exonum.binding.transport.Server;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.net.URISyntaxException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * An integration test of ServiceRuntime + Guice interop.
 */
@ExtendWith(MockitoExtension.class)
class ServiceRuntimeIntegrationTest {

  private static final int PORT = 25000;

  @Nested
  class WithTestFrameworkModule {

    private Injector rootInjector;
    private ServiceRuntime serviceRuntime;

    @BeforeEach
    void setUp() {
      rootInjector = Guice.createInjector(new TestFrameworkModule());
      serviceRuntime = new ServiceRuntime(rootInjector, PORT);
    }

    @Test
    void startsServerOnInstantiation() {
      Server server = rootInjector.getInstance(Server.class);
      verify(server).start(PORT);
    }

    @Test
    void loadArtifactNoOp() throws URISyntaxException {
      String serviceArtifactLocation = "file:///tmp/foo-service.jar";
      String artifactId = serviceRuntime.loadArtifact(serviceArtifactLocation);
      assertThat(artifactId).isEqualTo("com.acme:any-service:1.0.0");
    }

    @Test
    void createService() {
      String serviceModuleName = TestServiceModule.class.getName();
      UserServiceAdapter service = serviceRuntime
          .createService("artifactId/not-relevant", serviceModuleName);

      assertNotNull(service, "service");
      assertThat(service.getId()).isEqualTo(ID);
      assertThat(service.getName()).isEqualTo(NAME);
    }
  }

  @Test
  void failsToCreateWithNonSingletonServerBinding() {
    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(Server.class).toProvider(() -> mock(Server.class));
      }
    });
    assertThrows(IllegalArgumentException.class, () -> new ServiceRuntime(injector, PORT));
  }
}

class TestFrameworkModule extends AbstractModule {

  @Override
  protected void configure() {
    bindToSingletonMock(Server.class);
    bindToSingletonMock(ViewFactory.class);
  }

  private <T> void bindToSingletonMock(Class<T> type) {
    bind(type).toProvider(() -> mock(type)).in(Singleton.class);
  }
}

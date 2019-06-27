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

package com.exonum.binding.core.runtime;

import static com.exonum.binding.core.runtime.FrameworkModule.SERVICE_WEB_SERVER_PORT;
import static com.exonum.binding.core.runtime.ServiceRuntimeTest.PORT;
import static com.google.inject.name.Names.named;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.core.service.adapters.UserServiceAdapter;
import com.exonum.binding.core.service.adapters.ViewFactory;
import com.exonum.binding.core.transport.Server;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * An integration test of ServiceRuntime + Guice interop.
 */
@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD) // MockitoExtension is not thread-safe: see mockito/1630
class ServiceRuntimeTest {

  static final int PORT = 25000;

  @Nested
  class WithTestFrameworkModule {

    private Injector rootInjector;
    private ServiceRuntime serviceRuntime;

    @BeforeEach
    void setUp() {
      rootInjector = Guice.createInjector(Stage.DEVELOPMENT, new TestFrameworkModule());
      serviceRuntime = rootInjector.getInstance(ServiceRuntime.class);
    }

    @Test
    void startsServerOnInstantiation() {
      Server server = rootInjector.getInstance(Server.class);
      verify(server).start(PORT);
    }

    @Test
    void loadArtifact() throws Exception {
      String serviceArtifactLocation = "/tmp/foo-service.jar";
      ServiceLoader serviceLoader = rootInjector.getInstance(ServiceLoader.class);
      ServiceId serviceId = ServiceId.of("com.acme", "foo-service", "1.0.0");
      LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
          .newInstance(serviceId, new ReflectiveModuleSupplier(TestServiceModule.class));
      when(serviceLoader.loadService(Paths.get(serviceArtifactLocation)))
          .thenReturn(serviceDefinition);

      String artifactId = serviceRuntime.loadArtifact(serviceArtifactLocation);
      assertThat(artifactId).isEqualTo(serviceId.toString());
    }

    @Test
    void loadArtifactFailed() throws Exception {
      String serviceArtifactLocation = "/tmp/foo-service.jar";
      ServiceLoader serviceLoader = rootInjector.getInstance(ServiceLoader.class);
      ServiceLoadingException exception = new ServiceLoadingException("Boom");
      when(serviceLoader.loadService(Paths.get(serviceArtifactLocation)))
          .thenThrow(exception);

      Exception actual = assertThrows(ServiceLoadingException.class,
          () -> serviceRuntime.loadArtifact(serviceArtifactLocation));
      assertThat(actual).isSameAs(exception);
    }

    @Test
    void createService() throws Exception {
      String serviceId = "com.acme:foo-service:1.0.0";
      ServiceLoader serviceLoader = rootInjector.getInstance(ServiceLoader.class);
      LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
          .newInstance(ServiceId.parseFrom(serviceId),
              new ReflectiveModuleSupplier(TestServiceModule.class));
      when(serviceLoader.findService(ServiceId.parseFrom(serviceId)))
          .thenReturn(Optional.of(serviceDefinition));

      UserServiceAdapter service = serviceRuntime.createService(serviceId);

      assertThat(service).isNotNull();
      assertThat(service.getId()).isEqualTo(TestService.ID);
      assertThat(service.getName()).isEqualTo(TestService.NAME);
    }

    @Test
    void createServiceUnknownService() {
      String serviceId = "com.acme:foo-service:1.0.0";
      ServiceLoader serviceLoader = rootInjector.getInstance(ServiceLoader.class);
      when(serviceLoader.findService(ServiceId.parseFrom(serviceId)))
          .thenReturn(Optional.empty());

      Exception e = assertThrows(IllegalArgumentException.class,
          () -> serviceRuntime.createService(serviceId));
      assertThat(e).hasMessageFindingMatch("Unknown.+artifact");
      assertThat(e).hasMessageContaining(serviceId);
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
    ServiceLoader serviceLoader = mock(ServiceLoader.class);
    Server s1 = mock(Server.class);
    assertThrows(IllegalArgumentException.class,
        () -> new ServiceRuntime(injector, serviceLoader, s1, PORT));
  }
}

/** A test framework module that will instantiate mocks-singletons on demand. */
class TestFrameworkModule extends AbstractModule {

  @Override
  protected void configure() {
    bindToSingletonMock(ServiceLoader.class);
    bindToSingletonMock(Server.class);
    bind(Integer.class).annotatedWith(named(SERVICE_WEB_SERVER_PORT))
        .toInstance(PORT);
    bindToSingletonMock(ViewFactory.class);
  }

  private <T> void bindToSingletonMock(Class<T> type) {
    bind(type).toProvider(() -> mock(type))
        .in(Singleton.class);
  }
}

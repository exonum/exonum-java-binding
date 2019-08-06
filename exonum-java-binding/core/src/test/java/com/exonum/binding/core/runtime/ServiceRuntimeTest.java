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

import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.service.Service;
import com.exonum.binding.core.service.ServiceModule;
import com.exonum.binding.core.service.TransactionConverter;
import com.exonum.binding.core.service.adapters.ViewFactory;
import com.exonum.binding.core.storage.database.Database;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.TemporaryDb;
import com.exonum.binding.core.transport.Server;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
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
  static final String TEST_NAME = "test_service_name";
  static final int TEST_ID = 17;

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
    void deployCorrectArtifact() throws Exception {
      ServiceArtifactId serviceId = ServiceArtifactId.of("com.acme", "foo-service", "1.0.0");
      Path serviceArtifactLocation = Paths.get("/tmp/foo-service.jar");
      ServiceLoader serviceLoader = rootInjector.getInstance(ServiceLoader.class);
      LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
          .newInstance(serviceId, TestServiceModule::new);
      when(serviceLoader.loadService(serviceArtifactLocation))
          .thenReturn(serviceDefinition);

      serviceRuntime.deployArtifact(serviceId, serviceArtifactLocation);

      verify(serviceLoader).loadService(serviceArtifactLocation);
    }

    @Test
    void deployArtifactWrongId() throws Exception {
      ServiceArtifactId actualId = ServiceArtifactId.of("com.acme", "actual", "1.0.0");
      Path serviceArtifactLocation = Paths.get("/tmp/foo-service.jar");
      ServiceLoader serviceLoader = rootInjector.getInstance(ServiceLoader.class);
      LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
          .newInstance(actualId, TestServiceModule::new);
      when(serviceLoader.loadService(serviceArtifactLocation))
          .thenReturn(serviceDefinition);

      ServiceArtifactId expectedId = ServiceArtifactId.of("com.acme", "expected", "1.0.0");

      Exception actual = assertThrows(ServiceLoadingException.class,
          () -> serviceRuntime.deployArtifact(expectedId, serviceArtifactLocation));
      assertThat(actual).hasMessageContainingAll(actualId.toString(), expectedId.toString(),
          serviceArtifactLocation.toString());

      // Check the service artifact is unloaded
      verify(serviceLoader).unloadService(actualId);
    }

    @Test
    void deployArtifactFailed() throws Exception {
      ServiceArtifactId serviceId = ServiceArtifactId.of("com.acme", "foo-service", "1.0.0");
      Path serviceArtifactLocation = Paths.get("/tmp/foo-service.jar");
      ServiceLoader serviceLoader = rootInjector.getInstance(ServiceLoader.class);
      ServiceLoadingException exception = new ServiceLoadingException("Boom");
      when(serviceLoader.loadService(serviceArtifactLocation))
          .thenThrow(exception);

      Exception actual = assertThrows(ServiceLoadingException.class,
          () -> serviceRuntime.deployArtifact(serviceId, serviceArtifactLocation));
      assertThat(actual).isSameAs(exception);
    }

    @Test
    void createService() {
      ServiceArtifactId artifactId = ServiceArtifactId.parseFrom("com.acme:foo-service:1.0.0");
      ServiceLoader serviceLoader = rootInjector.getInstance(ServiceLoader.class);
      LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
          .newInstance(artifactId, TestServiceModule::new);
      when(serviceLoader.findService(artifactId))
          .thenReturn(Optional.of(serviceDefinition));

      // Create the service from the artifact
      ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
          TEST_ID, artifactId);
      serviceRuntime.createService(instanceSpec);

      // Check it is present and has configured name
      Optional<ServiceWrapper> serviceOpt = serviceRuntime.findService(TEST_NAME);

      assertThat(serviceOpt).isPresent();
      ServiceWrapper serviceWrapper = serviceOpt.get();
      assertThat(serviceWrapper.getName()).isEqualTo(TEST_NAME);
    }

    @Test
    void createServiceDuplicate() {
      ServiceArtifactId artifactId = ServiceArtifactId.parseFrom("com.acme:foo-service:1.0.0");
      ServiceLoader serviceLoader = rootInjector.getInstance(ServiceLoader.class);
      LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
          .newInstance(artifactId, TestServiceModule::new);
      when(serviceLoader.findService(artifactId))
          .thenReturn(Optional.of(serviceDefinition));

      // Create the service from the artifact
      ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
          TEST_ID, artifactId);
      serviceRuntime.createService(instanceSpec);

      // Try to create another service with the same service instance specification
      Exception e = assertThrows(IllegalArgumentException.class,
          () -> serviceRuntime.createService(instanceSpec));

      assertThat(e).hasMessageContaining("name");
      assertThat(e).hasMessageContaining(TEST_NAME);
    }

    @Test
    void createServiceUnknownService() {
      ServiceArtifactId artifactId = ServiceArtifactId.parseFrom("com.acme:foo-service:1.0.0");
      ServiceLoader serviceLoader = rootInjector.getInstance(ServiceLoader.class);
      when(serviceLoader.findService(artifactId)).thenReturn(Optional.empty());

      ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
          TEST_ID, artifactId);

      Exception e = assertThrows(IllegalArgumentException.class,
          () -> serviceRuntime.createService(instanceSpec));
      assertThat(e).hasMessageFindingMatch("Unknown.+artifact");
      assertThat(e).hasMessageContaining(String.valueOf(artifactId));
    }

    @Test
    void configureService() throws CloseFailuresException {
      // todo: Extract in some setup method as we have quite some tests requiring a service?
      ServiceArtifactId artifactId = ServiceArtifactId.parseFrom("com.acme:foo-service:1.0.0");
      ServiceLoader serviceLoader = rootInjector.getInstance(ServiceLoader.class);
      LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
          .newInstance(artifactId, MockServiceModule::new);
      when(serviceLoader.findService(artifactId))
          .thenReturn(Optional.of(serviceDefinition));

      // Create the service from the artifact
      ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
          TEST_ID, artifactId);
      serviceRuntime.createService(instanceSpec);

      try (Database database = TemporaryDb.newInstance();
          Cleaner cleaner = new Cleaner()) {
        Fork view = database.createFork(cleaner);
        Properties properties = new Properties();

        // Configure the service
        serviceRuntime.configureService(TEST_NAME, view, properties);

        // Check the service was configured
        Service service = serviceRuntime.findService(TEST_NAME)
            .map(ServiceWrapper::getService)
            .get();
        verify(service).configure(view, properties);
      }
    }

    @Test
    void configureNonExistingService() throws CloseFailuresException {
      try (Database database = TemporaryDb.newInstance();
          Cleaner cleaner = new Cleaner()) {
        Fork view = database.createFork(cleaner);
        Properties properties = new Properties();

        // Configure the service
        Exception e = assertThrows(IllegalArgumentException.class,
            () -> serviceRuntime.configureService(TEST_NAME, view, properties));

        assertThat(e).hasMessageContaining(TEST_NAME);
      }
    }

    @Test
    void stopService() {
      ServiceArtifactId artifactId = ServiceArtifactId.parseFrom("com.acme:foo-service:1.0.0");
      ServiceLoader serviceLoader = rootInjector.getInstance(ServiceLoader.class);
      LoadedServiceDefinition serviceDefinition = LoadedServiceDefinition
          .newInstance(artifactId, TestServiceModule::new);
      when(serviceLoader.findService(artifactId))
          .thenReturn(Optional.of(serviceDefinition));

      // Create the service from the artifact
      ServiceInstanceSpec instanceSpec = ServiceInstanceSpec.newInstance(TEST_NAME,
          TEST_ID, artifactId);
      serviceRuntime.createService(instanceSpec);

      // Stop the service
      serviceRuntime.stopService(TEST_NAME);

      // Check no service with such name remains registered
      Optional<ServiceWrapper> serviceOpt = serviceRuntime.findService(TEST_NAME);
      assertThat(serviceOpt).isEmpty();
    }

    @Test
    void stopNonExistingService() {
      assertThrows(IllegalArgumentException.class, () -> serviceRuntime.stopService(TEST_NAME));
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

abstract class AbstractMockingModule extends AbstractModule {
  <T> void bindToSingletonMock(Class<T> type) {
    bind(type).toProvider(() -> mock(type))
        .in(Singleton.class);
  }
}

/** A test framework module that will instantiate mocks-singletons on demand. */
class TestFrameworkModule extends AbstractMockingModule {

  @Override
  protected void configure() {
    bindToSingletonMock(ServiceLoader.class);
    bindToSingletonMock(Server.class);
    bind(Integer.class).annotatedWith(named(SERVICE_WEB_SERVER_PORT))
        .toInstance(PORT);
    bindToSingletonMock(ViewFactory.class);
  }
}

/**
 * A service module providing mocks-singletons of {@link Service} and {@link TransactionConverter}.
 */
class MockServiceModule extends AbstractMockingModule implements ServiceModule {

  @Override
  protected void configure() {
    bindToSingletonMock(Service.class);
    bindToSingletonMock(TransactionConverter.class);
  }
}

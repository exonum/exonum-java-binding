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

import static com.exonum.binding.runtime.FrameworkModule.SERVICE_WEB_SERVER_PORT;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.service.ServiceModule;
import com.exonum.binding.service.adapters.UserServiceAdapter;
import com.exonum.binding.transport.Server;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A service runtime. It manages the services required for operation of Exonum services (e.g., a
 * {@link Server}; allows the native code to load and unload artifacts (JAR archives with Exonum
 * services), create and stop services defined in the loaded artifacts.
 *
 * <p>This class is thread-safe and does not support client-side locking.
 * The thread-safety is provided because the class is a singleton and may be provided to
 * other objects. Currently, however, there is a single injection point where ServiceRuntime
 * is instantiated (during bootstrap) and it is used by the native runtime only in a single-threaded
 * context, hence thread-safety isn't <em>strictly</em> required, but rather provided to avoid
 * possible errors if it is ever accessed by other objects.
 */
@Singleton
public final class ServiceRuntime {

  private static final Logger logger = LogManager.getLogger(ServiceRuntime.class);

  private final Injector frameworkInjector;
  private final ServiceLoader serviceLoader;
  private final Object lock = new Object();

  /**
   * Creates a new runtime with the given framework injector. Starts the server on instantiation;
   * never stops it.
   *
   * @param frameworkInjector the injector that has been configured with the Exonum framework
   *     bindings. It serves as a parent for service injectors
   * @param serviceLoader a loader of service artifacts
   * @param server a web server providing transport to Java services
   * @param serverPort a port for the web server providing transport to Java services
   */
  @Inject
  public ServiceRuntime(Injector frameworkInjector, ServiceLoader serviceLoader, Server server,
      @Named(SERVICE_WEB_SERVER_PORT) int serverPort) {
    this.frameworkInjector = checkNotNull(frameworkInjector);
    this.serviceLoader = checkNotNull(serviceLoader);

    // Start the server
    checkServerIsSingleton(server, frameworkInjector);
    server.start(serverPort);
  }

  private void checkServerIsSingleton(Server s1, Injector frameworkInjector) {
    Server s2 = frameworkInjector.getInstance(Server.class);
    checkArgument(s1.equals(s2), "%s is not configured as singleton: s1=%s, s2=%s", Server.class,
        s1, s2);
  }

  /**
   * Loads an artifact from the specified location. The loading involves verification of the
   * artifact (i.e., that it is a valid Exonum service; includes a valid service factory).
   *
   * @param serviceArtifactPath a {@linkplain java.nio.file.Path filesystem path} from which
   *     to load the service artifact
   * @return a unique service artifact identifier that must be specified in subsequent operations
   *     with it
   * @throws ServiceLoadingException if it failed to load an artifact; or if the given artifact is
   *     already loaded
   */
  public String loadArtifact(String serviceArtifactPath) throws ServiceLoadingException {
    Path serviceArtifactLocation = Paths.get(serviceArtifactPath);
    try {
      ServiceId serviceId;
      synchronized (lock) {
        LoadedServiceDefinition loadedServiceDefinition = serviceLoader
            .loadService(serviceArtifactLocation);
        serviceId = loadedServiceDefinition.getId();
      }

      // Debug as 'loading' is an impl detail (until dynamic services)
      logger.debug("Loaded an artifact ({}) from {}", serviceId, serviceArtifactPath);

      return serviceId.toString();
    } catch (Throwable e) {
      logger.error("Failed to load an artifact from {}", serviceArtifactPath, e);
      throw e;
    }
  }

  /**
   * Creates a new service instance of the given type.
   *
   * @param artifactId a unique identifier of the loaded artifact
   * @return a new service
   * @throws IllegalArgumentException if the artifactId is unknown
   * @throws RuntimeException if it failed to instantiate the service
   */
  public UserServiceAdapter createService(String artifactId) {
    try {
      ServiceId serviceId = ServiceId.parseFrom(artifactId);

      UserServiceAdapter service;
      synchronized (lock) {
        LoadedServiceDefinition serviceDefinition = serviceLoader.findService(serviceId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown artifactId: " + artifactId));
        Supplier<ServiceModule> serviceModuleSupplier = serviceDefinition.getModuleSupplier();
        Module serviceModule = serviceModuleSupplier.get();
        Injector serviceInjector = frameworkInjector.createChildInjector(serviceModule);
        service = serviceInjector.getInstance(UserServiceAdapter.class);
      }

      logger.info("Created {} service (id={}, artifactId={})", service.getName(), service.getId(),
          artifactId);

      return service;
    } catch (Throwable e) {
      logger.error("Failed to create a service {} instance", artifactId, e);
      throw e;
    }
  }

  // TODO: unloadArtifact and stopService, once they can be used/ECR-2275
}

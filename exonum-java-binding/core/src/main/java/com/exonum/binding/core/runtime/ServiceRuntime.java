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
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.core.service.ServiceModule;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.transport.Server;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
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
  // todo: test the iteration order.
  /**
   * The list of services. It is stored in a sorted map that offers the same iteration order
   * on all nodes with the same services, which is required for correct beforeCommit
   * operation.
   */
  private final SortedMap<String, ServiceWrapper> services;
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
    services = new TreeMap<>();

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
   * Loads a Java service artifact from the specified location. The loading involves verification
   * of the artifact (i.e., that it is a valid Exonum service; includes a valid service factory).
   *
   * @param id a service artifact identifier; artifacts with non-equal ids will be rejected
   * @param location a filesystem path from which to load the service artifact
   * @throws ServiceLoadingException if it failed to load an artifact; or if the given artifact is
   *     already loaded
   */
  public void deployArtifact(ServiceArtifactId id, Path location)
      throws ServiceLoadingException {
    try {
      synchronized (lock) {
        LoadedServiceDefinition loadedServiceDefinition = serviceLoader
            .loadService(location);
        ServiceArtifactId actualId = loadedServiceDefinition.getId();

        // Check the artifact has the correct identifier in its metadata
        if (!actualId.equals(id)) {
          // Unload the artifact
          serviceLoader.unloadService(actualId);
          throw new ServiceLoadingException(
              String.format("The artifact loaded from (%s) has wrong id (%s) in "
                  + "metadata. Expected: %s", location, actualId, id));
        }
      }

      logger.info("Loaded an artifact ({}) from {}", id, location);
    } catch (Throwable e) {
      logger.error("Failed to load an artifact {} from {}", id, location, e);
      throw e;
    }
  }

  /**
   * Creates a new service instance with the given specification. This method registers
   * the service API.
   *
   * @param instanceSpec a service instance specification; must reference a deployed artifact
   * @throws IllegalArgumentException if the artifactId is unknown
   * @throws RuntimeException if it failed to instantiate the service
   */
  public void createService(ServiceInstanceSpec instanceSpec) {
    ServiceArtifactId artifactId = instanceSpec.getArtifactId();
    try {
      synchronized (lock) {
        // Check no such service in the runtime
        String name = instanceSpec.getName();
        checkArgument(!findService(name).isPresent(),
            "Service with name '%s' already created: %s", name, services.get(name));

        // Find the service definition
        LoadedServiceDefinition serviceDefinition = serviceLoader.findService(artifactId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown artifactId: " + artifactId));

        // Instantiate the service
        Supplier<ServiceModule> serviceModuleSupplier = serviceDefinition.getModuleSupplier();
        Module serviceModule = serviceModuleSupplier.get();
        Module serviceFrameworkModule = new ServiceFrameworkModule(instanceSpec);
        // todo: Reconsider the relationships between the framework injector and the child.
        //   Currently the child injector sees everything from the parent, but it does not
        //   seem to need that, the service needs only a well-defined subset of dependencies.
        Injector serviceInjector = frameworkInjector.createChildInjector(serviceModule,
            serviceFrameworkModule);
        ServiceWrapper service = serviceInjector.getInstance(ServiceWrapper.class);

        // Connect it to the API
        // todo: Add this from UserServiceAdapter
        // todo: reconsider once the core decides on the interface. It does not seem correct at all
        //   to provide external access to service operations **before** configuration.

        // Register it in the runtime
        services.put(name, service);
      }

      // Log the instantiation event
      String name = instanceSpec.getName();
      int id = instanceSpec.getId();
      logger.info("Created {} service (id={}, artifactId={})", name, id, artifactId);
    } catch (Exception e) {
      logger.error("Failed to create a service {} instance", instanceSpec, e);
      throw e;
    }
  }

  /**
   * Configures the service instance.
   *
   * @param name the name of the started service
   * @param view a database view to apply configuration
   * @param configuration service instance configuration parameters
   */
  // todo: Shall we use a more strict (and opaque) type for service id? E.g., to be able to change
  //   it later (though the runtime is an internal API that we can change at any time)?
  //   Also, shall we use the numeric id for performance reasons (= the native won't have to
  //   convert the Rust string into the Java string, but pass an int).
  public void configureService(String name, Fork view, Properties configuration) {
    // todo: Shall we control the state transitions (e.g., that one can configure only the
    //   started service and only once)? It feels like an overkill for an implementation of
    //   a framework abstraction.
    synchronized (lock) {
      checkService(name);

      try {
        ServiceWrapper service = services.get(name);
        service.configure(view, configuration);
      } catch (Exception e) {
        logger.error("Service {} configuration with parameters {} failed",
            name, configuration, e);
        throw e;
      }
    }

    logger.info("Configured service {} with parameters {}", name, configuration);
  }

  /**
   * Stops a started service.
   *
   * <p><strong>The present implementation is rather limited, as the core uses it only
   * if the service configuration failed, hence this operation is (a) blocking; (b) complete —
   * there is no preceding notification that the service is about to be stopped, no guarantees
   * on transaction processing; (c) no artifact unloading.</strong>
   * @param name the name of the started service
   */
  public void stopService(String name) {
    synchronized (lock) {
      checkService(name);
      // todo: disconnect from the API if it was connected

      // Unregister the service
      services.remove(name);
    }

    logger.info("Stopped service {}", name);
  }

  /** Checks that the service with the given name is started in this runtime. */
  private void checkService(String name) {
    checkArgument(findService(name).isPresent(), "No service with name %s in the Java runtime",
        name);
  }

  @VisibleForTesting
  Optional<ServiceWrapper> findService(String name) {
    return Optional.ofNullable(services.get(name));
  }

  // TODO: unloadArtifact and stopService, once they can be used/ECR-2275
}

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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.service.BlockCommittedEvent;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.transaction.ExecutionException;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transport.Server;
import com.exonum.core.messages.Runtime.ErrorKind;
import com.exonum.core.messages.Runtime.InstanceState;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
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
public final class ServiceRuntime implements AutoCloseable {

  @VisibleForTesting
  static final String API_ROOT_PATH = "/api/services";
  private static final Logger logger = LogManager.getLogger(ServiceRuntime.class);

  private final ServiceLoader serviceLoader;
  private final ServicesFactory servicesFactory;
  private final RuntimeTransport runtimeTransport;
  private final Path artifactsDir;
  /**
   * The active services indexed by their name. It is stored in a sorted map that offers
   * the same iteration order on all nodes with the same services, which is useful
   * for logging purposes.
   */
  private final SortedMap<String, ServiceWrapper> services = new TreeMap<>();
  /**
   * Same active services, indexed by their numeric identifier.
   * @see ServiceInstanceSpec#getId()
   */
  private final Map<Integer, ServiceWrapper> servicesById = new HashMap<>();
  private final Object lock = new Object();

  private Node node;

  /**
   * Creates a new Java service runtime.
   *
   * @param serviceLoader a loader of service artifacts
   * @param servicesFactory the factory of services
   * @param runtimeTransport a web server providing transport to Java services
   * @param artifactsDir the directory in which administrators place and from which
   *     the service runtime loads service artifacts; may not exist at instantiation time
   */
  @Inject
  public ServiceRuntime(ServiceLoader serviceLoader, ServicesFactory servicesFactory,
      RuntimeTransport runtimeTransport,
      @Named(FrameworkModule.SERVICE_RUNTIME_ARTIFACTS_DIRECTORY) Path artifactsDir) {
    this.serviceLoader = checkNotNull(serviceLoader);
    this.servicesFactory = checkNotNull(servicesFactory);
    this.runtimeTransport = checkNotNull(runtimeTransport);
    this.artifactsDir = checkNotNull(artifactsDir);
  }

  /**
   * Initializes the runtime with the given node. Starts the transport for Java services.
   */
  public void initialize(Node node) {
    synchronized (lock) {
      checkState(this.node == null, "Invalid attempt to replace already set node (%s) with %s",
          this.node, node);
      this.node = checkNotNull(node);

      // Start the server
      runtimeTransport.start();
    }
  }

  /**
   * Loads a Java service artifact from the specified file. The loading involves verification
   * of the artifact (i.e., that it is a valid Exonum service; includes a valid service factory).
   *
   * @param id a service artifact identifier; artifacts with non-equal ids will be rejected
   * @param filename a filename of the service artifact in the directory for artifacts
   * @throws ServiceLoadingException if it failed to load an artifact; or if the given artifact is
   *     already loaded
   */
  public void deployArtifact(ServiceArtifactId id, String filename)
      throws ServiceLoadingException {
    try {
      synchronized (lock) {
        // Check the artifacts dir exists
        checkState(Files.isDirectory(artifactsDir), "Artifacts dir (%s) does not exist or is not "
            + "a directory: check the runtime configuration", artifactsDir);
        Path artifactLocation = artifactsDir.resolve(filename);

        // Load the service artifact
        LoadedServiceDefinition loadedServiceDefinition = serviceLoader
            .loadService(artifactLocation);

        // Check the artifact has the correct identifier in its metadata
        ServiceArtifactId actualId = loadedServiceDefinition.getId();
        if (!actualId.equals(id)) {
          // Unload the artifact
          serviceLoader.unloadService(actualId);
          throw new ServiceLoadingException(
              String.format("The artifact loaded from (%s) has wrong id (%s) in "
                  + "metadata. Expected id: %s", filename, actualId, id));
        }
      }

      logger.info("Loaded an artifact ({}) from {}", id, filename);
    } catch (Throwable e) {
      logger.error("Failed to load an artifact {} from {}", id, filename, e);
      throw e;
    }
  }

  /**
   * Returns true if an artifact with the given id is currently deployed in this runtime.
   * @param id a service artifact identifier
   */
  public boolean isArtifactDeployed(ServiceArtifactId id) {
    synchronized (lock) {
      return serviceLoader.findService(id)
          .isPresent();
    }
  }

  /**
   * Starts registration of a new service instance with the given specification.
   * It involves the initial configuration of the service instance with the given parameters.
   * The instance is not registered until
   * {@link #updateInstanceStatus(ServiceInstanceSpec, InstanceState.Status)}
   * is invoked with the {@code Status=Active}.
   *
   * @param fork a database access to apply configuration
   * @param instanceSpec a service instance specification; must reference a deployed artifact
   * @param configuration service instance configuration parameters as a serialized protobuf
   *     message
   * @throws IllegalArgumentException if the service is already started; or its artifact
   *     is not deployed
   * @throws ExecutionException if such exception occurred in the service constructor;
   *     must be translated into an error of kind {@link ErrorKind#SERVICE}
   * @throws UnexpectedExecutionException if any other exception occurred in
   *     the service constructor; it is included as cause. The cause must be translated
   *     into an error of kind {@link ErrorKind#UNEXPECTED}
   * @throws RuntimeException if the runtime failed to instantiate the service for other reason
   */
  public void initiateAddingService(Fork fork, ServiceInstanceSpec instanceSpec,
      byte[] configuration) {
    try {
      synchronized (lock) {
        // Create a new service
        ServiceWrapper service = createServiceInstance(instanceSpec);

        // Initialize it
        service.initialize(fork, new ServiceConfiguration(configuration));
      }

      // Log the initialization event
      logger.info("Initialized a new service: {}", instanceSpec);
    } catch (Exception e) {
      logger.error("Failed to initialize a service {} instance with parameters {}",
          instanceSpec, configuration, e);
      throw e;
    }
  }

  /**
   * Initiates resuming of previously stopped service instance. Service instance artifact could
   * be upgraded in advance to bring some new functionality.
   *
   * @param fork a database fork to apply changes to
   * @param instanceSpec a service instance specification; must reference a deployed artifact
   * @param arguments a service arguments as a serialized protobuf message
   * @throws IllegalArgumentException if the given service instance is active; or its artifact
   *     is not deployed
   * @throws ExecutionException if such exception occurred in the service method;
   *     must be translated into an error of kind {@link ErrorKind#SERVICE}
   * @throws UnexpectedExecutionException if any other exception occurred in
   *     the service method; it is included as cause. The cause must be translated
   *     into an error of kind {@link ErrorKind#UNEXPECTED}
   * @throws RuntimeException if the runtime failed to resume the service for other reason
   */
  public void initializeResumingService(Fork fork, ServiceInstanceSpec instanceSpec,
      byte[] arguments) {
    try {
      synchronized (lock) {
        checkStoppedService(instanceSpec.getId());
        ServiceWrapper service = createServiceInstance(instanceSpec);
        service.resume(fork, arguments);
      }
      logger.info("Resumed service: {}", instanceSpec);
    } catch (Exception e) {
      logger.error("Failed to resume a service {} instance with parameters {}",
          instanceSpec, arguments, e);
      throw e;
    }
  }

  /**
   * Modifies the state of the given service instance at the runtime either by activation it or
   * stopping. The service instance should be successfully initialized
   * by {@link #initiateAddingService(Fork, ServiceInstanceSpec, byte[])} in advance.
   * Activation leads to the service instance registration, allowing subsequent operations on it:
   * transactions, API requests.
   * Stopping leads to the service disabling i.e. stopped service does not execute transactions,
   * process events, provide APIs, etc. But the service data still exists.
   *
   * @param instanceSpec a service instance specification; must reference a deployed artifact
   * @param instanceStatus a new status of the service instance
   * @throws IllegalArgumentException if activating already active service; or its artifact
   *     is not deployed; or unrecognized service status received
   */
  public void updateInstanceStatus(ServiceInstanceSpec instanceSpec,
      InstanceState.Status instanceStatus) {
    synchronized (lock) {
      switch (instanceStatus) {
        case ACTIVE:
          activateService(instanceSpec);
          break;
        case STOPPED:
          stopService(instanceSpec);
          break;
        default:
          String msg = String.format("Unexpected status %s received for the service %s",
              instanceStatus.name(), instanceSpec.getName());
          logger.error(msg);
          throw new IllegalArgumentException(msg);
      }
    }
  }

  private void activateService(ServiceInstanceSpec instanceSpec) {
    try {
      // Create a previously added service
      ServiceWrapper service = createServiceInstance(instanceSpec);
      // Register it in the runtime
      registerService(service);
      // Connect its API
      connectServiceApi(service);
      logger.info("Activated a service: {}", instanceSpec);
    } catch (Exception e) {
      logger.error("Failed to activate a service {} instance", instanceSpec, e);
      throw e;
    }
  }

  private void stopService(ServiceInstanceSpec instanceSpec) {
    String name = instanceSpec.getName();
    Optional<ServiceWrapper> activeService = findService(name);
    if (activeService.isPresent()) {
      ServiceWrapper service = activeService.get();
      service.requestToStop();
      runtimeTransport.disconnectServiceApi(service);
      unRegisterService(service);
      logger.info("Stopped a service: {}", instanceSpec);
    } else {
      logger.warn("There is no active service with the given name {}. "
          + "Possibly restoring services state after reboot?", name);
    }
  }

  private ServiceWrapper createServiceInstance(ServiceInstanceSpec instanceSpec) {
    // Check no such service in the runtime
    String name = instanceSpec.getName();
    checkArgument(!findService(name).isPresent(),
        "Service with name '%s' already created: %s", name, services.get(name));

    // Find the service definition
    ServiceArtifactId artifactId = instanceSpec.getArtifactId();
    LoadedServiceDefinition serviceDefinition = serviceLoader.findService(artifactId)
        .orElseThrow(() -> new IllegalArgumentException("Unknown artifactId: " + artifactId));

    // Instantiate the service
    return servicesFactory.createService(serviceDefinition, instanceSpec,
        new MultiplexingNodeDecorator(node));
  }

  private void registerService(ServiceWrapper service) {
    String name = service.getName();
    services.put(name, service);

    int id = service.getId();
    servicesById.put(id, service);
  }

  private void unRegisterService(ServiceWrapper service) {
    String name = service.getName();
    services.remove(name);

    int id = service.getId();
    servicesById.remove(id);
  }

  /**
   * Connects the API of a started service to the web-server.
   */
  private void connectServiceApi(ServiceWrapper service) {
    try {
      runtimeTransport.connectServiceApi(service);
    } catch (Exception e) {
      // The core currently requires not to propagate the exception to it, but handle it
      // in the runtime. Such behaviour is user-hostile as we hide the error in logs instead
      // of communicating it immediately and prominently (by stopping the service).
      // It is to be reconsidered when service termination is implemented.
      logger.error("Failed to connect service {} public API. "
          + "Its HTTP handlers will likely be inaccessible", service.getName(), e);
    }
  }

  /**
   * Executes a transaction belonging to the given service.
   *
   * @param serviceId the numeric identifier of the service instance to which the transaction
   *     belongs
   * @param interfaceName a fully-qualified name of the interface in which the transaction
   *     is defined, or empty string if it is defined in the service directly (implicit interface)
   * @param txId the transaction type identifier
   * @param arguments the serialized transaction arguments
   * @param fork a native fork object
   * @param callerServiceId the id of the caller service if transaction is invoked by other
   *     service. Currently only applicable to invocations of Configure interface methods
   * @param txMessageHash the hash of the transaction message
   * @param authorPublicKey the public key of the transaction author
   * @throws ExecutionException if such exception occurred in the transaction;
   *     must be translated into an error of kind {@link ErrorKind#SERVICE}
   * @throws UnexpectedExecutionException if any other exception occurred in
   *     the transaction; it is included as cause. The cause must be translated
   *     into an error of kind {@link ErrorKind#UNEXPECTED}
   * @throws IllegalArgumentException if any argument is not valid (e.g., unknown service)
   * @see com.exonum.binding.core.transaction.Transaction
   */
  public void executeTransaction(int serviceId, String interfaceName, int txId,
      byte[] arguments, Fork fork, int callerServiceId, HashCode txMessageHash,
      PublicKey authorPublicKey) {
    synchronized (lock) {
      ServiceWrapper service = getServiceById(serviceId);
      String serviceName = service.getName();
      TransactionContext context = TransactionContext.builder()
          .fork(fork)
          .txMessageHash(txMessageHash)
          .authorPk(authorPublicKey)
          .serviceName(serviceName)
          .serviceId(serviceId)
          .build();
      try {
        service.executeTransaction(interfaceName, txId, arguments, callerServiceId, context);
      } catch (Exception e) {
        logger.info("Transaction execution failed (service={}, txId={}, txMessageHash={})",
            service.getName(), txId, context.getTransactionMessageHash(), e);
        throw e;
      }
    }
  }

  /**
   * Performs the after transactions operation on the specified service in this runtime.
   *
   * @param serviceId the id of the service on which to perform the operation
   * @param fork a fork allowing the runtime and the service to modify the database state.
   * @throws ExecutionException if such exception occurred in the transaction;
   *     must be translated into an error of kind {@link ErrorKind#SERVICE}
   * @throws UnexpectedExecutionException if any other exception occurred in
   *     the transaction; it is included as cause. The cause must be translated
   *     into an error of kind {@link ErrorKind#UNEXPECTED}
   * @throws IllegalArgumentException if any argument is not valid (e.g., unknown service)
   */
  public void afterTransactions(int serviceId, Fork fork) {
    synchronized (lock) {
      ServiceWrapper service = getServiceById(serviceId);
      try {
        service.afterTransactions(fork);
      } catch (Exception e) {
        logger.error("Service {} threw exception in afterTransactions."
            + " Any changes will be rolled-back", service.getName(), e);
        throw e;
      }
    }
  }

  /**
   * Notifies the services in the runtime of the block commit event.
   */
  public void afterCommit(BlockCommittedEvent event) {
    synchronized (lock) {
      for (ServiceWrapper service : services.values()) {
        try {
          // todo: [ECR-3436] BCE carries a Snapshot which is based on a cleaner, which gets
          //   re-used by all services. If the total number of native proxies they create is large,
          //   that may result in excessive memory usage. Some ways to solve this:
          //   1. Take a handle, create a fresh snapshot for each service — but will need hacks
          //   to destroy the native peer once.
          //   2. Support Snapshot copying with new cleaners — but that breaks index de-duplication
          //   (though for snapshots that mustn't be an issue).
          //   -
          //   As a side note, 'excessive memory usage' may occur in any *single* transaction/
          //   read request/service life-cycle method, it just has higher probability when
          //   we invoke a number of such 'foreign' (to framework) methods with no intermediate
          //   clean-up.
          service.afterCommit(event);
        } catch (Exception e) {
          // Log, but do not re-throw either immediately or later
          logger.error("Service {} threw an exception in its afterCommit handler of {}",
              service.getName(), event, e);
        }
      }
    }
  }

  /**
   * Stops this runtime. It will stop the server providing transport to services,
   * remove all services and unload their artifacts. The operation is irreversible;
   * the runtime may not be used after this operation completes.
   *
   * @throws InterruptedException if an interrupt was requested
   */
  public void shutdown() throws InterruptedException {
    synchronized (lock) {
      try {
        logger.info("Shutting down the runtime");

        // Stop the server
        stopServer();

        // Clear the services
        clearServices();

        // Finally, when no service classes remain in use, unload the service artifacts
        unloadArtifacts();

        // Free-up native resources
        if (node != null) {
          node.close();
        }
        logger.info("The runtime shutdown complete");
      } catch (Exception e) {
        logger.error("Shutdown failure", e);
        throw e;
      }
    }
  }

  private void stopServer() throws InterruptedException {
    try {
      logger.info("Requesting the HTTP server to stop");
      runtimeTransport.close();
      logger.info("Stopped the HTTP server");
    } catch (InterruptedException e) {
      // An interruption was requested
      logger.warn("Interrupted before completion");
      throw e;
    } catch (Exception e) {
      // Log and go on — such exceptions shall not affect the process
      logger.error("Exception occurred whilst stopping the server", e);
    }
  }

  private void clearServices() {
    services.clear();
    servicesById.clear();
  }

  private void unloadArtifacts() {
    try {
      logger.info("Unloading the artifacts");
      serviceLoader.unloadAll();
      logger.info("Unloaded the artifacts");
    } catch (IllegalStateException e) {
      // Log and go on — such exception shall not affect the process
      logger.error("Unload failure", e);
    }
  }

  @Override
  public void close() throws InterruptedException {
    shutdown();
  }

  private ServiceWrapper getServiceById(Integer serviceId) {
    checkActiveService(serviceId);
    return servicesById.get(serviceId);
  }

  /** Checks that the service with the given id is started in this runtime. */
  private void checkActiveService(Integer serviceId) {
    checkArgument(servicesById.containsKey(serviceId),
        "No service with id=%s in the Java runtime", serviceId);
  }

  /** Checks that the service with the given id is not active in this runtime. */
  private void checkStoppedService(Integer serviceId) {
    ServiceWrapper activeService = servicesById.get(serviceId);
    checkArgument(activeService == null,
        "Service with id=%s should be stopped, but actually active. "
            + "Found active service instance: %s", serviceId, activeService);
  }

  @VisibleForTesting
  Optional<ServiceWrapper> findService(String name) {
    return Optional.ofNullable(services.get(name));
  }
}

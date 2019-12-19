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
import static java.util.stream.Collectors.toList;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.runtime.ServiceRuntimeProtos.ServiceRuntimeStateHashes;
import com.exonum.binding.core.runtime.ServiceRuntimeProtos.ServiceStateHashes;
import com.exonum.binding.core.service.BlockCommittedEvent;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transaction.TransactionExecutionException;
import com.exonum.binding.core.transport.Server;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
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
   * the same iteration order on all nodes with the same services, which is required
   * for correct operation of beforeCommit and {@link #getStateHashes(Snapshot)}.
   */
  private final SortedMap<String, ServiceWrapper> services = new TreeMap<>();
  /**
   * Same active services, indexed by their numeric identifier.
   * @see ServiceInstanceSpec#getId()
   */
  private final Map<Integer, ServiceWrapper> servicesById = new HashMap<>();
  private final Object lock = new Object();

  // todo: [ECR-2334] Ensure the Node is properly destroyed when the runtime is stopped
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
   * The instance is not registered until {@link #commitService(ServiceInstanceSpec)}
   * is invoked.
   *
   * @param fork a database view to apply configuration
   * @param instanceSpec a service instance specification; must reference a deployed artifact
   * @param configuration service instance configuration parameters as a serialized protobuf
   *     message
   * @throws IllegalArgumentException if the service is already started; or its artifact
   *     is not deployed
   * @throws RuntimeException if it failed to instantiate the service;
   *     or if the service initialization failed
   */
  public void startAddingService(Fork fork, ServiceInstanceSpec instanceSpec,
      byte[] configuration) {
    try {
      synchronized (lock) {
        // Create a new service
        ServiceWrapper service = createService(instanceSpec);

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
   * Adds a service instance to the runtime after it has been successfully initialized
   * in {@link #startAddingService(Fork, ServiceInstanceSpec, byte[])}. This operation
   * completes the service instance registration, allowing subsequent operations on it:
   * transactions, API requests.
   *
   * @param instanceSpec a service instance specification; must reference a deployed artifact
   * @throws IllegalArgumentException if the service is already started; or its artifact
   *     is not deployed
   */
  public void commitService(ServiceInstanceSpec instanceSpec) {
    try {
      synchronized (lock) {
        // Create a previously added service
        ServiceWrapper service = createService(instanceSpec);
        // Register it in the runtime
        registerService(service);
        // Connect its API
        connectServiceApi(service);
      }

      logger.info("Added a service: {}", instanceSpec);
    } catch (Exception e) {
      logger.error("Failed to add a service {} instance", instanceSpec, e);
      throw e;
    }
  }

  private ServiceWrapper createService(ServiceInstanceSpec instanceSpec) {
    // Check no such service in the runtime
    String name = instanceSpec.getName();
    checkArgument(!findService(name).isPresent(),
        "Service with name '%s' already created: %s", name, services.get(name));

    // Find the service definition
    ServiceArtifactId artifactId = instanceSpec.getArtifactId();
    LoadedServiceDefinition serviceDefinition = serviceLoader.findService(artifactId)
        .orElseThrow(() -> new IllegalArgumentException("Unknown artifactId: " + artifactId));

    // Instantiate the service
    return servicesFactory.createService(serviceDefinition, instanceSpec, node);
  }

  private void registerService(ServiceWrapper service) {
    String name = service.getName();
    services.put(name, service);

    int id = service.getId();
    servicesById.put(id, service);
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
   */
  public void executeTransaction(int serviceId, String interfaceName, int txId,
      byte[] arguments, Fork fork, int callerServiceId, HashCode txMessageHash,
      PublicKey authorPublicKey)
      throws TransactionExecutionException {
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
   * Returns the state hashes of this runtime and the services registered in it as a protobuf
   * message.
   *
   * @param snapshot the snapshot of the current database state
   */
  public ServiceRuntimeStateHashes getStateHashes(Snapshot snapshot) {
    synchronized (lock) {
      // Collect the service state hashes
      List<ServiceStateHashes> serviceStateHashes = services.values().stream()
          .map(service -> getServiceStateHashes(service, snapshot))
          .collect(toList());

      return ServiceRuntimeStateHashes.newBuilder()
              // The runtime itself does not have any state hashes at the moment.
              .addAllServiceStateHashes(serviceStateHashes)
              .build();
    }
  }

  private ServiceStateHashes getServiceStateHashes(ServiceWrapper service, Snapshot snapshot) {
    List<HashCode> stateHashes = service.getStateHashes(snapshot);
    List<ByteString> stateHashesAsBytes = stateHashes.stream()
        .map(hash -> ByteString.copyFrom(hash.asBytes()))
        .collect(toList());
    return ServiceStateHashes.newBuilder()
        .setInstanceId(service.getId())
        .addAllStateHashes(stateHashesAsBytes)
        .build();
  }

  /**
   * Performs the before commit operation on the specified service in this runtime.
   *
   * @param serviceId the id of the service on which to perform the operation
   * @param fork a fork allowing the runtime and the service to modify the database state.
   */
  public void beforeCommit(int serviceId, Fork fork) {
    synchronized (lock) {
      ServiceWrapper service = getServiceById(serviceId);
      try {
        service.beforeCommit(fork);
      } catch (Exception e) {
        logger.error("Service {} threw exception in beforeCommit. Any changes will be rolled-back",
            service.getName(), e);
        throw e;
      }
    }
  }

  /**
   * Notifies the services in the runtime of the block commit event.
   */
  public void afterCommit(BlockCommittedEvent event) {
    synchronized (lock) {
      for (ServiceWrapper service: services.values()) {
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
    checkService(serviceId);
    return servicesById.get(serviceId);
  }

  /** Checks that the service with the given id is started in this runtime. */
  private void checkService(Integer serviceId) {
    checkArgument(servicesById.containsKey(serviceId),
        "No service with id=%s in the Java runtime", serviceId);
  }

  @VisibleForTesting
  Optional<ServiceWrapper> findService(String name) {
    return Optional.ofNullable(services.get(name));
  }

  // TODO: unloadArtifact and stopService, once they can be used/ECR-2275
}

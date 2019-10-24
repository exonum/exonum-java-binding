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
import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.runtime.ServiceRuntimeProtos.ServiceRuntimeStateHashes;
import com.exonum.binding.core.runtime.ServiceRuntimeProtos.ServiceStateHashes;
import com.exonum.binding.core.service.BlockCommittedEvent;
import com.exonum.binding.core.service.Configuration;
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
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public final class ServiceRuntime {

  @VisibleForTesting
  static final String API_ROOT_PATH = "/api/services";
  private static final Logger logger = LogManager.getLogger(ServiceRuntime.class);

  private final ServiceLoader serviceLoader;
  private final ServicesFactory servicesFactory;
  private final Server server;
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

  /**
   * Creates a new runtime with the given framework injector. Starts the server on instantiation;
   * never stops it.
   *
   * @param serviceLoader a loader of service artifacts
   * @param servicesFactory the factory of services
   * @param server a web server providing transport to Java services
   * @param artifactsDir the directory in which administrators place and from which
   *     the service runtime loads service artifacts; may not exist at instantiation time
   * @param serverPort a port for the web server providing transport to Java services
   */
  @Inject
  public ServiceRuntime(ServiceLoader serviceLoader, ServicesFactory servicesFactory, Server server,
      @Named(FrameworkModule.SERVICE_RUNTIME_ARTIFACTS_DIRECTORY) Path artifactsDir,
      @Named(SERVICE_WEB_SERVER_PORT) int serverPort) {
    this.serviceLoader = checkNotNull(serviceLoader);
    this.servicesFactory = checkNotNull(servicesFactory);
    this.server = checkNotNull(server);
    this.artifactsDir = checkNotNull(artifactsDir);

    // Start the server
    server.start(serverPort);
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
        ServiceWrapper service = servicesFactory.createService(serviceDefinition, instanceSpec);

        // Register it in the runtime
        registerService(service);
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

  private void registerService(ServiceWrapper service) {
    String name = service.getName();
    services.put(name, service);

    int id = service.getId();
    servicesById.put(id, service);
  }

  /**
   * Performs an initial configuration of the service instance.
   *
   * @param id the id of the started service
   * @param view a database view to apply configuration
   * @param configuration service instance configuration parameters as a serialized protobuf
   *     message
   */
  public void initializeService(Integer id, Fork view, byte[] configuration) {
    synchronized (lock) {
      ServiceWrapper service = getServiceById(id);
      try {
        Configuration config = new ServiceConfiguration(configuration);
        service.initialize(view, config);
      } catch (Exception e) {
        String name = service.getName();
        logger.error("Service {} configuration with parameters {} failed",
            name, configuration, e);
        throw e;
      }
      logger.info("Configured service {} with parameters {}", service.getName(), configuration);
    }
  }

  /**
   * Stops a started service.
   *
   * <p><strong>The present implementation is rather limited, as the core uses it only
   * if the service configuration failed, hence this operation is (a) blocking; (b) complete —
   * there is no preceding notification that the service is about to be stopped, no guarantees
   * on transaction processing; (c) no artifact unloading.</strong>
   * @param id the id of the started service
   */
  public void stopService(Integer id) {
    synchronized (lock) {
      ServiceWrapper service = getServiceById(id);

      // Unregister the service
      unregisterService(service);

      logger.info("Stopped service {}", service.getName());
    }
  }

  private void unregisterService(ServiceWrapper service) {
    services.remove(service.getName());
    servicesById.remove(service.getId());
  }

  /**
   * Executes a transaction belonging to the given service.
   * @param serviceId the numeric identifier of the service instance to which the transaction
   *     belongs
   * @param txId the transaction type identifier
   * @param arguments the serialized transaction arguments
   * @param fork a native fork object
   * @param txMessageHash the hash of the transaction message
   * @param authorPublicKey the public key of the transaction author
   */
  public void executeTransaction(Integer serviceId, int txId, byte[] arguments,
                                 Fork fork, HashCode txMessageHash, PublicKey authorPublicKey)
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
        service.executeTransaction(txId, arguments, context);
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
   * Performs the before commit operation for all services in the runtime.
   *
   * @param fork a fork allowing the runtime and the service to modify the database state.
   *             Must allow checkpoints and rollbacks.
   */
  public void beforeCommit(Fork fork) {
    synchronized (lock) {
      try {
        for (ServiceWrapper service : services.values()) {
          fork.createCheckpoint();
          try {
            service.beforeCommit(fork);
          } catch (Exception e) {
            logger.error("Service {} threw exception in beforeCommit. Any changes are rolled-back",
                service.getName(), e);
            fork.rollback();
          }
        }
      } catch (Exception e) {
        logger.error("Unexpected exception in beforeCommit", e);
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
   * Connects the API of successfully started and configured services to the web-server.
   *
   * @param serviceIds the ids of the services to connect; must not be empty
   * @param node a node allowing to access the database state and submit transactions
   */
  public void connectServiceApis(int[] serviceIds, Node node) {
    checkArgument(serviceIds.length != 0, "ServiceRuntime native proxy must not invoke this "
        + "method each block as that would result in <blockchain height> nodes and, eventually, "
        + "an OutOfMemoryError");
    // todo: [ECR-2334] Ensure the Node is properly destroyed when each service is stopped
    synchronized (lock) {
      for (Integer serviceId : serviceIds) {
        connectServiceApi(serviceId, node);
      }
    }
  }

  private void connectServiceApi(Integer serviceId, Node node) {
    ServiceWrapper service = getServiceById(serviceId);

    try {
      // Create the service API handlers
      Router router = server.createRouter();
      service.createPublicApiHandlers(node, router);

      // Mount the service handlers
      String serviceApiPath = createServiceApiPath(service);
      server.mountSubRouter(serviceApiPath, router);

      // Log the endpoints
      logApiMountEvent(service, serviceApiPath, router);
    } catch (Exception e) {
      // The core currently requires not to propagate the exception to it, but handle it
      // in the runtime. Such behaviour is user-hostile as we hide the error in logs instead
      // of communicating it immediately and prominently (by stopping the service).
      // It is to be reconsidered when service termination is implemented.
      logger.error("Failed to connect service {} public API. "
          + "Its HTTP handlers will likely be inaccessible", service.getName(), e);
    }
  }

  private static String createServiceApiPath(ServiceWrapper service) {
    String servicePathFragment = service.getPublicApiRelativePath();
    return API_ROOT_PATH + "/" + servicePathFragment;
  }

  private void logApiMountEvent(ServiceWrapper service, String serviceApiPath, Router router) {
    List<Route> serviceRoutes = router.getRoutes();
    if (serviceRoutes.isEmpty()) {
      // The service has no API: nothing to log
      return;
    }

    String serviceName = service.getName();
    int port = server.getActualPort().orElse(0);
    // Currently the API is mounted on *all* interfaces, see VertxServer#start
    logger.info("Service {} API is mounted at :{}{}", serviceName, port, serviceApiPath);

    // Log the full path to one of the service endpoint
    serviceRoutes.stream()
        .map(Route::getPath)
        .filter(Objects::nonNull) // null routes are possible in failure handlers, for instance
        .findAny()
        .ifPresent(someRoute ->
            logger.info("    E.g.: http://127.0.0.1:{}{}", port, serviceApiPath + someRoute)
        );
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

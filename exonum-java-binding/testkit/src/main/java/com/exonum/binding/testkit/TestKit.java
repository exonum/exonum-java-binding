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

import static com.exonum.binding.app.ServiceRuntimeBootstrap.DEPENDENCY_REFERENCE_CLASSES;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.common.messages.DeployArguments;
import com.exonum.binding.common.runtime.ServiceArtifactId;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.core.blockchain.Block;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.blockchain.BlockchainData;
import com.exonum.binding.core.blockchain.serialization.BlockSerializer;
import com.exonum.binding.core.proxy.AbstractCloseableNativeProxy;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.runtime.DispatcherSchema;
import com.exonum.binding.core.runtime.FrameworkModule;
import com.exonum.binding.core.runtime.ServiceRuntimeAdapter;
import com.exonum.binding.core.service.BlockCommittedEvent;
import com.exonum.binding.core.service.Configuration;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.service.Service;
import com.exonum.binding.core.storage.database.Prefixed;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.indices.KeySetIndexProxy;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.core.testkit.internal.TestKitProtos.TestKitServiceInstances;
import com.exonum.binding.core.transaction.RawTransaction;
import com.exonum.binding.core.transport.Server;
import com.exonum.binding.core.util.LibraryLoader;
import com.exonum.messages.core.runtime.Base;
import com.exonum.messages.core.runtime.Lifecycle;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.protobuf.Any;
import com.google.protobuf.MessageLite;
import io.vertx.ext.web.Router;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;

/**
 * TestKit for testing blockchain services. It offers simple network configuration emulation
 * (with no real network setup). Although it is possible to add several validator nodes to this
 * network, only one node will create the service instances, execute their operations (e.g.,
 * {@linkplain Service#afterCommit(BlockCommittedEvent)} method logic), and provide access to its
 * state.
 *
 * <p>Only the emulated node has a pool of unconfirmed transactions where a service can submit new
 * transaction messages through {@link Node#submitTransaction(RawTransaction)}; or the test
 * code through {@link #createBlockWithTransactions(TransactionMessage...)}. All transactions
 * from the pool are committed when a new block is created with {@link #createBlock()}.
 *
 * <p>When TestKit is created, Exonum blockchain instance is initialized — service instances are
 * {@linkplain Service#initialize(com.exonum.binding.core.blockchain.BlockchainData, Configuration)
 * initialized} and genesis block is committed.
 * Then the {@linkplain Service#createPublicApiHandlers(Node, Router) public API handlers} are
 * created.
 *
 * @see <a href="https://exonum.com/doc/version/0.13-rc.2/get-started/test-service/">TestKit documentation</a>
 * @see <a href="https://exonum.com/doc/version/0.13-rc.2/advanced/consensus/specification/#pool-of-unconfirmed-transactions">Pool of Unconfirmed Transactions</a>
 */
public final class TestKit extends AbstractCloseableNativeProxy {

  static {
    LibraryLoader.load();
  }

  /**
   * The maximum number of validators supported by TestKit when a time oracle is enabled. The time
   * oracle does not work in a TestKit with a higher number of validators because the time oracle
   * requires the majority of those validators to submit transactions with time updates, but only a
   * single emulated node submits them.
   */
  public static final short MAX_VALIDATOR_COUNT_WITH_ENABLED_TIME_SERVICE = 3;
  @VisibleForTesting
  static final short MAX_SERVICE_NUMBER = 256;
  @VisibleForTesting
  static final int MAX_SERVICE_INSTANCE_ID = 1023;
  @VisibleForTesting
  static final Any DEFAULT_CONFIGURATION = Any.getDefaultInstance();
  // Set 0 as a server port so it will assign a random suitable port by default
  private static final int SERVER_PORT = 0;
  private static final Serializer<Block> BLOCK_SERIALIZER = BlockSerializer.INSTANCE;

  private final int port;

  @VisibleForTesting
  final Cleaner snapshotCleaner = new Cleaner("TestKit#getSnapshot");

  private TestKit(long nativeHandle, int port) {
    super(nativeHandle, true);
    this.port = port;
  }

  private static TestKit newInstance(TestKitServiceInstances serviceInstances,
                                     EmulatedNodeType nodeType, short validatorCount,
                                     @Nullable TimeServiceSpec timeServiceSpec,
                                     Path artifactsDirectory) {
    // Create the test network
    Injector frameworkInjector = createTestRuntimeInjector(artifactsDirectory);
    ServiceRuntimeAdapter serviceRuntimeAdapter =
        frameworkInjector.getInstance(ServiceRuntimeAdapter.class);
    boolean isAuditorNode = nodeType == EmulatedNodeType.AUDITOR;
    long nativeHandle = nativeCreateTestKit(serviceInstances.toByteArray(), isAuditorNode,
            validatorCount, timeServiceSpec, serviceRuntimeAdapter);

    try {
      // Get the actual port: it must have been set as testkit initialized the runtimes.
      Server serviceServer = frameworkInjector.getInstance(Server.class);
      int port = serviceServer.getActualPort()
          .orElseThrow(() -> new IllegalStateException(
              "No port set after testkit has been created"));
      return new TestKit(nativeHandle, port);
    } catch (Exception e) {
      // Free the native object and re-throw
      nativeFreeTestKit(nativeHandle);
      throw e;
    }
  }

  private static Injector createTestRuntimeInjector(Path artifactsDirectory) {
    Module frameworkModule = new FrameworkModule(artifactsDirectory, SERVER_PORT,
        DEPENDENCY_REFERENCE_CLASSES);
    return Guice.createInjector(frameworkModule);
  }

  /**
   * Deploys and creates a single service with no configuration and with a single validator node in
   * this TestKit network.
   *
   * @param artifactId the id of the artifact
   * @param artifactFilename a filename of the service artifact in the directory for artifacts
   * @param serviceName the name of the service
   * @param serviceId the id of the service, must be in range
   *     [0; {@value #MAX_SERVICE_INSTANCE_ID}]
   * @param artifactsDirectory the directory from which the service runtime loads service
   *     artifacts
   *
   * @throws IllegalArgumentException if serviceId is not in range
   *     [0; {@value #MAX_SERVICE_INSTANCE_ID}]
   */
  public static TestKit forService(ServiceArtifactId artifactId, String artifactFilename,
                                   String serviceName, int serviceId, Path artifactsDirectory) {
    return new Builder()
        .withNodeType(EmulatedNodeType.VALIDATOR)
        .withDeployedArtifact(artifactId, artifactFilename)
        .withService(artifactId, serviceName, serviceId)
        .withArtifactsDirectory(artifactsDirectory)
        .build();
  }

  /**
   * Creates a block with the given transaction(s). Transactions are applied in the lexicographical
   * order of their hashes. In-pool transactions will be ignored.
   *
   * @return created block
   * @throws RuntimeException if any transaction does not belong to a started service
   *     (i.e., has an unknown service id)
   */
  public Block createBlockWithTransactions(TransactionMessage... transactions) {
    return createBlockWithTransactions(asList(transactions));
  }

  /**
   * Creates a block with the given transactions. Transactions are applied in the lexicographical
   * order of their hashes. In-pool transactions will be ignored.
   *
   * @return created block
   * @throws RuntimeException if any transaction does not belong to a started service
   *     (i.e., has an unknown service id)
   */
  public Block createBlockWithTransactions(Iterable<TransactionMessage> transactions) {
    List<TransactionMessage> messageList = ImmutableList.copyOf(transactions);
    byte[][] transactionMessagesArr = messageList.stream()
        .map(TransactionMessage::toBytes)
        .toArray(byte[][]::new);
    byte[] block = nativeCreateBlockWithTransactions(nativeHandle.get(), transactionMessagesArr);
    return BLOCK_SERIALIZER.fromBytes(block);
  }

  /**
   * Creates a block with all in-pool transactions. Transactions are applied in the lexicographical
   * order of their hashes.
   *
   * @return created block
   */
  public Block createBlock() {
    byte[] block = nativeCreateBlock(nativeHandle.get());
    return BLOCK_SERIALIZER.fromBytes(block);
  }

  /**
   * Returns a list of in-pool transactions. Please note that the order of transactions in pool
   * does not necessarily match the order in which the clients submitted the messages.
   */
  public List<TransactionMessage> getTransactionPool() {
    return findTransactionsInPool(transactionMessage -> true);
  }

  /**
   * Returns a list of in-pool transactions that match the given predicate.
   */
  public List<TransactionMessage> findTransactionsInPool(Predicate<TransactionMessage> predicate) {
    return applySnapshot((snapshot) -> {
      Blockchain blockchain = Blockchain.newInstance(snapshot);
      MapIndex<HashCode, TransactionMessage> txMessages = blockchain.getTxMessages();
      KeySetIndexProxy<HashCode> poolTxsHashes = blockchain.getTransactionPool();
      return poolTxsHashes.stream()
          .map(txMessages::get)
          .filter(predicate)
          .collect(toList());
    });
  }

  /**
   * Returns the snapshot of the current state of the service data for a service
   * with the given name.
   *
   * <p>A shortcut for {@link BlockchainData#getExecutingServiceData()}.</p>
   *
   * @param serviceName the name of the service instance to which data the access is needed
   * @throws IllegalArgumentException if the service with the given name does not exist
   */
  public Prefixed getServiceData(String serviceName) {
    return getBlockchainData(serviceName).getExecutingServiceData();
  }

  /**
   * Returns the snapshot of the current state of the blockchain data for a service
   * with the given name.
   *
   * @param serviceName the name of the service instance to which data the access is needed
   * @throws IllegalArgumentException if the service with the given name does not exist
   * @see #getSnapshot()
   * @see #getServiceData(String)
   */
  public BlockchainData getBlockchainData(String serviceName) {
    Snapshot snapshot = createSnapshot(snapshotCleaner);
    checkServiceExists(serviceName, snapshot);
    return BlockchainData.fromRawAccess(snapshot, serviceName);
  }

  private void checkServiceExists(String serviceName, Snapshot snapshot) {
    boolean hasService = new DispatcherSchema(snapshot)
            .serviceInstances()
            .containsKey(serviceName);
    if (!hasService) {
      throw new IllegalArgumentException("No service with the given name: " + serviceName);
    }
  }

  /**
   * Performs the given function with a snapshot of the current database state (i.e., the one that
   * corresponds to the latest committed block). In-pool (not yet processed) transactions are also
   * accessible with it in {@linkplain Blockchain#getTxMessages() blockchain}.
   *
   * <p>This method destroys the snapshot once the passed closure completes, compared to
   * {@link #getSnapshot()}, which disposes created snapshots only when TestKit is closed.
   *
   * @param snapshotFunction a function to execute
   * @see #applySnapshot(Function)
   */
  public void withSnapshot(Consumer<Snapshot> snapshotFunction) {
    applySnapshot(s -> {
      snapshotFunction.accept(s);
      return null;
    });
  }

  /**
   * Performs the given function with a snapshot of the current database state (i.e., the one that
   * corresponds to the latest committed block) and returns a result of its execution. In-pool
   * (not yet processed) transactions are also accessible with it in
   * {@linkplain Blockchain#getTxMessages() blockchain}.
   *
   * <p>This method destroys the snapshot once the passed closure completes, compared to
   * {@link #getSnapshot()}, which disposes created snapshots only when TestKit is closed.
   *
   * <p>Consider using {@link #withSnapshot(Consumer)} when returning the result of given function
   * is not needed.
   *
   * @param snapshotFunction a function to execute
   * @param <ResultT> a type the function returns
   * @return the result of applying the given function to the database state
   */
  public <ResultT> ResultT applySnapshot(Function<Snapshot, ResultT> snapshotFunction) {
    try (Cleaner cleaner = new Cleaner("TestKit#applySnapshot")) {
      Snapshot snapshot = createSnapshot(cleaner);
      return snapshotFunction.apply(snapshot);
    } catch (CloseFailuresException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns a snapshot of the current database state (i.e., the one that
   * corresponds to the latest committed block). In-pool (not yet processed) transactions are also
   * accessible with it in {@linkplain Blockchain#getTxMessages() blockchain}.
   *
   * <p>All created snapshots are deleted when this TestKit is {@linkplain #close() closed}.
   * It is forbidden to access the snapshots once the TestKit is closed.
   *
   * <p>If you need to create a large number (e.g. more than a hundred) of snapshots, it is
   * recommended to use {@link #withSnapshot(Consumer)} or {@link #applySnapshot(Function)}, which
   * destroy the snapshots once the passed closure completes.
   */
  public Snapshot getSnapshot() {
    return createSnapshot(snapshotCleaner);
  }

  private Snapshot createSnapshot(Cleaner cleaner) {
    long snapshotHandle = nativeCreateSnapshot(nativeHandle.get());
    return Snapshot.newInstance(snapshotHandle, cleaner);
  }

  /**
   * Returns the context of the node that the TestKit emulates (i.e., on which it instantiates and
   * executes services).
   */
  public EmulatedNode getEmulatedNode() {
    return nativeGetEmulatedNode(nativeHandle.get());
  }

  /**
   * Returns the TCP port on which the service REST API is mounted.
   */
  public int getPort() {
    return port;
  }

  @Override
  protected void disposeInternal() {
    try {
      snapshotCleaner.close();
    } catch (CloseFailuresException e) {
      throw new IllegalStateException(e);
    } finally {
      nativeFreeTestKit(nativeHandle.get());
    }
  }

  private static native long nativeCreateTestKit(byte[] services,
                                                 boolean auditor, short withValidatorCount,
                                                 TimeServiceSpec timeProviderSpec,
                                                 ServiceRuntimeAdapter serviceRuntimeAdapter);

  private native long nativeCreateSnapshot(long nativeHandle);

  private native byte[] nativeCreateBlock(long nativeHandle);

  private native byte[] nativeCreateBlockWithTransactions(long nativeHandle, byte[][] transactions);

  private native EmulatedNode nativeGetEmulatedNode(long nativeHandle);

  private static native void nativeFreeTestKit(long nativeHandle);

  /**
   * Creates a new builder for the TestKit. Note that this builder creates a single validator
   * network by default.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for the TestKit.
   */
  public static final class Builder {

    private EmulatedNodeType nodeType = EmulatedNodeType.VALIDATOR;
    private short validatorCount = 1;
    private Multimap<ServiceArtifactId, Lifecycle.InstanceInitParams> services =
            ArrayListMultimap.create();
    private HashMap<ServiceArtifactId, String> serviceArtifactFilenames = new HashMap<>();
    private Path artifactsDirectory;
    private TimeServiceSpec timeServiceSpec;

    private Builder() {}

    /**
     * Returns a shallow copy of this TestKit builder.
     *
     * <p>Note that the remaining mutable state are {@linkplain TimeProvider time providers}.
     */
    Builder shallowCopy() {
      Builder builder = new Builder()
          .withNodeType(nodeType)
          .withValidators(validatorCount)
          .withArtifactsDirectory(artifactsDirectory);
      builder.timeServiceSpec = timeServiceSpec;
      builder.services = MultimapBuilder.hashKeys().arrayListValues().build(services);
      builder.serviceArtifactFilenames = new HashMap<>(serviceArtifactFilenames);
      return builder;
    }

    /**
     * Sets the type of the main TestKit node - either validator or auditor. Note that
     * {@link Service#afterCommit(BlockCommittedEvent)} logic will only be called on the main
     * TestKit node of this type
     */
    public Builder withNodeType(EmulatedNodeType nodeType) {
      this.nodeType = nodeType;
      return this;
    }

    /**
     * Sets number of validator nodes in the TestKit network, should be positive. Note that
     * regardless of the configured number of validators, only a single service will be
     * instantiated. Equal to one by default.
     *
     * <p>Note that validator count should be
     * {@value #MAX_VALIDATOR_COUNT_WITH_ENABLED_TIME_SERVICE} or less if time service is enabled.
     *
     * @throws IllegalArgumentException if validatorCount is less than one
     */
    public Builder withValidators(short validatorCount) {
      checkArgument(validatorCount > 0, "TestKit network should have at least one validator node");
      this.validatorCount = validatorCount;
      return this;
    }

    /**
     * Adds a service artifact which would be deployed by the TestKit. Several service artifacts
     * can be added.
     *
     * <p>Once the service artifact is deployed, the service instances can be added with
     * {@link #withService(ServiceArtifactId, String, int, MessageLite)}.
     */
    public Builder withDeployedArtifact(
        ServiceArtifactId serviceArtifactId, String artifactFilename) {
      serviceArtifactFilenames.put(serviceArtifactId, artifactFilename);
      return this;
    }

    /**
     * Sets artifact directory.
     *
     * @param artifactsDirectory the directory from which the service runtime loads service
     *     artifacts
     */
    public Builder withArtifactsDirectory(Path artifactsDirectory) {
      this.artifactsDirectory = artifactsDirectory;
      return this;
    }

    /**
     * Adds a service specification with which the TestKit would create the corresponding service
     * instance. Several service specifications can be added. All services are started and
     * configured before the genesis block.
     *
     * <p>Note that the corresponding service artifact with equal serviceArtifactId should be
     * deployed with {@link #withDeployedArtifact(ServiceArtifactId, String)}.
     *
     * @param serviceArtifactId the id of the artifact
     * @param serviceName the name of the service
     * @param serviceId the id of the service, must be in range
     *     [0; {@value #MAX_SERVICE_INSTANCE_ID}]
     * @param configuration the service configuration parameters
     *
     * @throws IllegalArgumentException if serviceId is not in range
     *     [0; {@value #MAX_SERVICE_INSTANCE_ID}]
     * @throws IllegalArgumentException if service artifact with equal serviceArtifactId
     *     was not deployed
     */
    public Builder withService(ServiceArtifactId serviceArtifactId, String serviceName,
                               int serviceId, MessageLite configuration) {
      checkServiceId(serviceId, serviceName);
      checkServiceArtifactIsDeployed(serviceArtifactId);

      // Collect specifications of service instances in their protobuf representation.
      Base.InstanceSpec instanceSpec = Base.InstanceSpec.newBuilder()
          .setId(serviceId)
          .setName(serviceName)
          .setArtifact(artifactIdToProto(serviceArtifactId))
          .build();
      Lifecycle.InstanceInitParams params = Lifecycle.InstanceInitParams.newBuilder()
          .setInstanceSpec(instanceSpec)
          .setConstructor(configuration.toByteString())
          .build();
      services.put(serviceArtifactId, params);
      return this;
    }

    /**
     * Adds a service specification with which the TestKit would create the corresponding service
     * instance with no configuration. Several service specifications can be added. All
     * services are started and configured before the genesis block.
     *
     * <p>Note that the corresponding service artifact with equal serviceArtifactId should be
     * deployed with {@link #withDeployedArtifact(ServiceArtifactId, String)}.
     *
     * @param serviceArtifactId the id of the artifact
     * @param serviceName the name of the service
     * @param serviceId the id of the service, must be in range
     *     [0; {@value #MAX_SERVICE_INSTANCE_ID}]
     *
     * @throws IllegalArgumentException if serviceId is not in range
     *     [0; {@value #MAX_SERVICE_INSTANCE_ID}]
     * @throws IllegalArgumentException if service artifact with equal serviceArtifactId
     *     was not deployed
     */
    public Builder withService(ServiceArtifactId serviceArtifactId, String serviceName,
                               int serviceId) {
      return withService(serviceArtifactId, serviceName, serviceId, DEFAULT_CONFIGURATION);
    }

    private void checkServiceId(int serviceId, String serviceName) {
      checkArgument(0 <= serviceId && serviceId <= MAX_SERVICE_INSTANCE_ID,
          "Service (%s) id must be in range [0; %s], but was %s",
          serviceName, MAX_SERVICE_INSTANCE_ID, serviceId);
    }

    private void checkServiceArtifactIsDeployed(ServiceArtifactId serviceArtifactId) {
      checkArgument(serviceArtifactFilenames.containsKey(serviceArtifactId),
          "Service %s should be deployed first in order to be created", serviceArtifactId);
    }

    /**
     * Adds a time service specification with which the TestKit would create the corresponding
     * time service instance. Only a single time service specification can be added.
     *
     * <p>Note that validator count should be
     * {@value #MAX_VALIDATOR_COUNT_WITH_ENABLED_TIME_SERVICE} or less if time service is enabled.
     */
    public Builder withTimeService(String serviceName, int serviceId, TimeProvider timeProvider) {
      TimeProviderAdapter timeProviderAdapter = new TimeProviderAdapter(timeProvider);
      timeServiceSpec = new TimeServiceSpec(serviceName, serviceId, timeProviderAdapter);
      return this;
    }

    /**
     * Creates the TestKit instance.
     *
     * @throws IllegalArgumentException if validator count is invalid
     * @throws IllegalArgumentException if service number is invalid
     * @throws IllegalArgumentException if service artifacts were deployed, but no service
     *     instances with same service artifact id were created
     */
    public TestKit build() {
      checkCorrectServiceNumber(services.size());
      checkCorrectValidatorNumber();
      checkArtifactsDirectory();
      TestKitServiceInstances testKitServiceInstances = prepareServicesConfiguration();
      return newInstance(testKitServiceInstances, nodeType, validatorCount,
          timeServiceSpec, artifactsDirectory);
    }

    /**
     * Turn collections of artifacts and service instances into a
     * {@linkplain TestKitServiceInstances} object for native to work with.
     */
    private TestKitServiceInstances prepareServicesConfiguration() {
      checkDeployedArtifactsAreUsed();
      TestKitServiceInstances.Builder builder = TestKitServiceInstances.newBuilder();

      // Add specifications of artifacts to deploy.
      for (Map.Entry<ServiceArtifactId, String> entry : serviceArtifactFilenames.entrySet()) {
        ServiceArtifactId artifactId = entry.getKey();
        Base.ArtifactSpec artifactSpec = Base.ArtifactSpec.newBuilder()
            .setArtifact(artifactIdToProto(artifactId))
            .setPayload(DeployArguments.newBuilder()
                .setArtifactFilename(entry.getValue())
                .build()
                .toByteString())
            .build();

        builder.addArtifactSpecs(artifactSpec);
      }

      // Add specifications of service instances to start.
      for (Lifecycle.InstanceInitParams instanceInitParams : services.values()) {
        builder.addServiceSpecs(instanceInitParams);
      }

      return builder.build();
    }

    private Base.ArtifactId artifactIdToProto(ServiceArtifactId artifactId) {
      return Base.ArtifactId.newBuilder()
          .setRuntimeId(artifactId.getRuntimeId())
          .setName(artifactId.getName())
          .setVersion(artifactId.getVersion())
          .build();
    }

    private void checkDeployedArtifactsAreUsed() {
      Set<ServiceArtifactId> serviceArtifactIds = services.keySet();
      Set<ServiceArtifactId> deployedArtifactIds = serviceArtifactFilenames.keySet();
      Sets.SetView<ServiceArtifactId> unusedArtifacts =
          Sets.difference(deployedArtifactIds, serviceArtifactIds);
      checkArgument(unusedArtifacts.isEmpty(),
          "Following service artifacts were deployed, but not used for service instantiation: %s",
          unusedArtifacts);
    }

    private void checkCorrectValidatorNumber() {
      if (timeServiceSpec != null) {
        checkArgument(validatorCount <= MAX_VALIDATOR_COUNT_WITH_ENABLED_TIME_SERVICE,
            "Number of validators (%s) should be less than or equal to %s when TimeService is"
                + " instantiated.",
            validatorCount, MAX_VALIDATOR_COUNT_WITH_ENABLED_TIME_SERVICE);
      }
    }

    private void checkCorrectServiceNumber(int serviceCount) {
      checkArgument(0 <= serviceCount && serviceCount <= MAX_SERVICE_NUMBER,
          "Number of services must be in range [0; %s], but was %s",
          MAX_SERVICE_NUMBER, serviceCount);
    }

    private void checkArtifactsDirectory() {
      checkState(artifactsDirectory != null, "Artifacts directory was not set.");
    }
  }
}

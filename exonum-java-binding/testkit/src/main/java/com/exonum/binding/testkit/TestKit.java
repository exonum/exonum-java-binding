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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.core.blockchain.Block;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.blockchain.serialization.BlockSerializer;
import com.exonum.binding.core.proxy.AbstractCloseableNativeProxy;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.runtime.ServiceArtifactId;
import com.exonum.binding.core.runtime.ServiceRuntime;
import com.exonum.binding.core.service.BlockCommittedEvent;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.service.Service;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.indices.KeySetIndexProxy;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.core.transaction.RawTransaction;
import com.exonum.binding.core.util.LibraryLoader;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.vertx.ext.web.Router;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
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
 * {@linkplain Service#configure(Fork) initialized} and genesis block is committed.
 * Then the {@linkplain Service#createPublicApiHandlers(Node, Router) public API handlers} are
 * created.
 *
 * @see <a href="https://exonum.com/doc/version/0.12/get-started/test-service/">TestKit documentation</a>
 * @see <a href="https://exonum.com/doc/version/0.12/advanced/consensus/specification/#pool-of-unconfirmed-transactions">Pool of Unconfirmed Transactions</a>
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
  private static final Serializer<Block> BLOCK_SERIALIZER = BlockSerializer.INSTANCE;
  private static final short TIME_SERVICE_ID = 4;

  @VisibleForTesting
  final Cleaner snapshotCleaner = new Cleaner("TestKit#getSnapshot");

  private TestKit(long nativeHandle) {
    super(nativeHandle, true);
  }

  private static TestKit newInstance(List<TestKitServiceInstances> serviceInstances,
                                     EmulatedNodeType nodeType, short validatorCount,
                                     @Nullable TimeServiceSpec timeServiceSpec) {
    boolean isAuditorNode = nodeType == EmulatedNodeType.AUDITOR;
    long nativeHandle = nativeCreateTestKit(
        serviceInstances.toArray(new TestKitServiceInstances[0]), isAuditorNode, validatorCount,
        timeServiceSpec);
    return new TestKit(nativeHandle);
  }

  /**
   * Deploys and creates a single service with a single validator node in this TestKit network.
   */
  public static TestKit forService(ServiceArtifactId artifactId, String artifactFilename,
                                   String serviceName, int serviceId, Any configuration) {
    List<TestKitServiceInstances> testKitServiceInstances = createTestKitSingleServiceInstance(
        artifactId, artifactFilename, serviceName, serviceId, configuration);
    return newInstance(testKitServiceInstances, EmulatedNodeType.VALIDATOR, (short) 1, null);
  }

  private static List<TestKitServiceInstances> createTestKitSingleServiceInstance(
      ServiceArtifactId artifactId, String artifactFilename, String serviceName,
      int serviceId, Any configuration) {
    ServiceSpec serviceSpec =
        ServiceSpec.newInstance(serviceName, serviceId, configuration.toByteArray());
    TestKitServiceInstances testKitServiceInstances = TestKitServiceInstances.newInstance(
        artifactId.toString(), artifactFilename, new ServiceSpec[] {serviceSpec});
    return singletonList(testKitServiceInstances);
  }

  /**
   * Returns an instance of a service with the given service name and service class. Only
   * user-defined services can be requested, i.e., it is not possible to get an instance of a
   * built-in service such as the time oracle.
   *
   * @return the service instance or null if there is no service with such id
   * @throws IllegalArgumentException if the service with given id was not found or could not be
   *     cast to given class
   */
  public <T extends Service> T getService(String serviceName, Class<T> serviceClass) {
    // TODO: retrieve it from ServiceRuntime
//    Service service = serviceRuntime.findService(serviceName);
//    checkArgument(service != null, "Service with given name=%s was not found", serviceName);
//    checkArgument(service.getClass().equals(serviceClass),
//        "Service (name=%s, class=%s) cannot be cast to %s",
//        serviceName, service.getClass().getCanonicalName(), serviceClass.getCanonicalName());
//    return serviceClass.cast(service);
    return null;
  }

  /**
   * Creates a block with the given transaction(s). Transactions are applied in the lexicographical
   * order of their hashes. In-pool transactions will be ignored.
   *
   * @return created block
   * @throws IllegalArgumentException if transactions are malformed or don't belong to this
   *     service
   */
  public Block createBlockWithTransactions(TransactionMessage... transactions) {
    return createBlockWithTransactions(asList(transactions));
  }

  /**
   * Creates a block with the given transactions. Transactions are applied in the lexicographical
   * order of their hashes. In-pool transactions will be ignored.
   *
   * @return created block
   * @throws IllegalArgumentException if transactions are malformed or don't belong to this
   *     service
   */
  public Block createBlockWithTransactions(Iterable<TransactionMessage> transactions) {
    List<TransactionMessage> messageList = ImmutableList.copyOf(transactions);
    checkTransactions(messageList);
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
    List<TransactionMessage> inPoolTransactions = getTransactionPool();
    checkTransactions(inPoolTransactions);
    byte[] block = nativeCreateBlock(nativeHandle.get());
    return BLOCK_SERIALIZER.fromBytes(block);
  }

  private void checkTransactions(List<TransactionMessage> transactionMessages) {
    for (TransactionMessage transactionMessage: transactionMessages) {
      checkTransaction(transactionMessage);
    }
  }

  private void checkTransaction(TransactionMessage transactionMessage) {
    int serviceId = transactionMessage.getServiceId();
    // As transactions of time service might be submitted in TestKit that has that service
    // activated, those transactions should be considered valid, even though time service is not
    // contained in 'services'
    if (serviceId == TIME_SERVICE_ID) {
      return;
    }
//    if (!services.containsKey(serviceId)) {
//      String message = String.format("Unknown service id (%s) in transaction (%s)",
//          serviceId, transactionMessage);
//      throw new IllegalArgumentException(message);
//    }
//    Service service = services.get(serviceId);
//    RawTransaction rawTransaction = RawTransaction.fromMessage(transactionMessage);
    try {
      // TODO: retrieve TransactionConverter from ServiceWrapper of a corresponding service?
//      service.convertToTransaction(rawTransaction);
    } catch (Throwable conversionError) {
//      String message = String.format("Service (%s) with id=%s failed to convert transaction (%s)."
//          + " Make sure that the submitted transaction is correctly serialized, and the service's"
//          + " TransactionConverter implementation is correct and handles this transaction as"
//          + " expected.", serviceName, serviceId, transactionMessage);
//      throw new IllegalArgumentException(message, conversionError);
    }
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
    return applySnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      MapIndex<HashCode, TransactionMessage> txMessages = blockchain.getTxMessages();
      KeySetIndexProxy<HashCode> poolTxsHashes = blockchain.getTransactionPool();
      return stream(poolTxsHashes)
          .map(txMessages::get)
          .filter(predicate)
          .collect(toList());
    });
  }

  private static <T> Stream<T> stream(KeySetIndexProxy<T> setIndex) {
    return Streams.stream(setIndex);
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

  private static native long nativeCreateTestKit(TestKitServiceInstances[] services,
                                                 boolean auditor, short withValidatorCount,
                                                 TimeServiceSpec timeProvider);

  private native long nativeCreateSnapshot(long nativeHandle);

  private native ServiceRuntime nativeGetServiceRuntime(long nativeHandle);

  private native byte[] nativeCreateBlock(long nativeHandle);

  private native byte[] nativeCreateBlockWithTransactions(long nativeHandle, byte[][] transactions);

  private native EmulatedNode nativeGetEmulatedNode(long nativeHandle);

  private native void nativeFreeTestKit(long nativeHandle);

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
    private final Multimap<ServiceArtifactId, ServiceSpec> services = ArrayListMultimap.create();
    private final HashMap<ServiceArtifactId, String> serviceArtifactFilenames = new HashMap<>();
    private TimeServiceSpec timeServiceSpec;

    private Builder() {
    }

    /**
     * Returns a copy of this TestKit builder.
     */
    public Builder copy() {
      Builder builder = new Builder()
          .withNodeType(nodeType)
          .withValidators(validatorCount)
          .withTimeService(timeServiceSpec);
      for (Map.Entry<ServiceArtifactId, String> entry: serviceArtifactFilenames.entrySet()) {
        builder.withDeployedService(entry.getKey(), entry.getValue());
      }
      for (Map.Entry<ServiceArtifactId, ServiceSpec> entry: services.entries()) {
        try {
          ServiceSpec serviceSpec = entry.getValue();
          builder.withService(entry.getKey(), serviceSpec.getServiceName(),
              serviceSpec.getServiceId(), Any.parseFrom(serviceSpec.getConfiguration()));
        } catch (InvalidProtocolBufferException e) {
          throw new IllegalArgumentException(
              "Invalid deploy configuration for service " + entry.getKey(), e);
        }
      }
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
     * <p>Note that the corresponding service instance with equal serviceArtifactId should be
     * created with {@link #withService(ServiceArtifactId, String, int, Any)}.
     */
    public Builder withDeployedService(ServiceArtifactId serviceArtifactId, String artifactFilename) {
      serviceArtifactFilenames.put(serviceArtifactId, artifactFilename);
      return this;
    }

    /**
     * Adds a service specification with which the TestKit would create the corresponding service
     * instance. Several service specifications can be added.
     *
     * <p>Note that the corresponding service artifact with equal serviceArtifactId should be
     * deployed with {@link #withDeployedService(ServiceArtifactId, String)}.
     */
    public Builder withService(ServiceArtifactId serviceArtifactId,
                               String serviceName,
                               int serviceId,
                               Any configuration) {
      ServiceSpec serviceSpec =
          ServiceSpec.newInstance(serviceName, serviceId, configuration.toByteArray());
      services.put(serviceArtifactId, serviceSpec);
      return this;
    }

    /**
     * If called, will create a TestKit with time service enabled. The time service will be created
     * with given name and id and use the given {@linkplain TimeProvider} as a time source.
     *
     * <p>Note that validator count should be
     * {@value #MAX_VALIDATOR_COUNT_WITH_ENABLED_TIME_SERVICE} or less if time service is enabled.
     */
    public Builder withTimeService(TimeProvider timeProvider, String serviceName, int serviceId) {
      TimeProviderAdapter timeProviderAdapter = new TimeProviderAdapter(timeProvider);
      this.timeServiceSpec = TimeServiceSpec.newInstance(timeProviderAdapter, serviceName, serviceId);
      return this;
    }

    /**
     * If called, will create a TestKit with time service enabled. The time service will be created
     * with given {@linkplain TimeServiceSpec} as a specification.
     *
     * <p>Used in {@linkplain #copy()} method.
     *
     * <p>Note that validator count should be
     * {@value #MAX_VALIDATOR_COUNT_WITH_ENABLED_TIME_SERVICE} or less if time service is enabled.
     */
    private Builder withTimeService(TimeServiceSpec timeServiceSpec) {
      this.timeServiceSpec = timeServiceSpec;
      return this;
    }

    /**
     * Creates the TestKit instance.
     *
     * @throws IllegalArgumentException if validator count is invalid
     * @throws IllegalArgumentException if service number is invalid
     */
    public TestKit build() {
      checkCorrectServiceNumber(services.size());
      checkCorrectValidatorNumber();
      List<TestKitServiceInstances> testKitServiceInstances = mergeServiceSpec();
      return newInstance(testKitServiceInstances, nodeType, validatorCount, timeServiceSpec);
    }

    /**
     * Turn collection of service instances into a list of
     * {@linkplain TestKitServiceInstances} objects for native to work with.
     */
    private List<TestKitServiceInstances> mergeServiceSpec() {
      Set<ServiceArtifactId> serviceArtifactIds = services.keySet();
      // TODO: better error message?
      checkArgument(serviceArtifactIds.containsAll(serviceArtifactFilenames.keySet()),
          "All service instances that are deployed should also be instantiated"
              + " and vice versa.");
      return serviceArtifactIds.stream()
          .map(this::aggregateServiceSpecs)
          .collect(toList());
    }

    /**
     * Aggregates service instances specifications of a given service artifact id as a
     * {@linkplain TestKitServiceInstances} object.
     */
    private TestKitServiceInstances aggregateServiceSpecs(ServiceArtifactId artifactId) {
      String artifactFilename = serviceArtifactFilenames.get(artifactId);
      ServiceSpec[] serviceSpecs = services.get(artifactId).toArray(new ServiceSpec[0]);
      return TestKitServiceInstances.newInstance(
          artifactId.toString(), artifactFilename, serviceSpecs);
    }

    private void checkCorrectValidatorNumber() {
      if (timeServiceSpec != null) {
        checkArgument(validatorCount <= MAX_VALIDATOR_COUNT_WITH_ENABLED_TIME_SERVICE,
            "Number of validators (%s) should be less than or equal to %s when TimeService is"
                + " enabled.",
            validatorCount, MAX_VALIDATOR_COUNT_WITH_ENABLED_TIME_SERVICE);
      }
    }

    private void checkCorrectServiceNumber(int serviceCount) {
      checkArgument(0 < serviceCount && serviceCount <= MAX_SERVICE_NUMBER,
          "Number of services must be in range [1; %s], but was %s",
          MAX_SERVICE_NUMBER, serviceCount);
    }
  }
}

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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.not;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import com.exonum.binding.blockchain.Block;
import com.exonum.binding.blockchain.Blockchain;
import com.exonum.binding.blockchain.serialization.BlockSerializer;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.proxy.AbstractCloseableNativeProxy;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.runtime.ReflectiveModuleSupplier;
import com.exonum.binding.service.BlockCommittedEvent;
import com.exonum.binding.service.Node;
import com.exonum.binding.service.Service;
import com.exonum.binding.service.ServiceModule;
import com.exonum.binding.service.adapters.UserServiceAdapter;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.transaction.RawTransaction;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * TestKit for testing blockchain services. It offers simple network configuration emulation
 * (with no real network setup). Although it is possible to add several validator nodes to this
 * network, only one node will create the service instances, execute their operations (e.g.,
 * {@linkplain Service#afterCommit(BlockCommittedEvent)} method logic), and provide access to its
 * state.
 *
 * <p>Only the emulated node has a pool of unconfirmed transactions where a service can submit new
 * transaction messages through {@linkplain Node#submitTransaction(RawTransaction)}. All
 * transactions from the pool are committed when a new block is created with
 * {@link #createBlock()}.
 *
 * <p>When TestKit is created, Exonum blockchain instance is initialized - service instances are
 * {@linkplain UserServiceAdapter#initialize(long)} initialized} and genesis block is committed.
 * Then the {@linkplain UserServiceAdapter#mountPublicApiHandler(long)} public API handlers} are
 * created.
 *
 * @see <a href="https://exonum.com/doc/version/0.10/get-started/test-service/">TestKit documentation</a>
 *      <a href="https://exonum.com/doc/version/0.10/advanced/consensus/specification/#pool-of-unconfirmed-transactions">Pool of Unconfirmed Transactions</a>
 */
public final class TestKit extends AbstractCloseableNativeProxy {

  @VisibleForTesting
  static final short MAX_SERVICE_NUMBER = 256;
  private static final Serializer<Block> BLOCK_SERIALIZER = BlockSerializer.INSTANCE;

  private final Map<Short, Service> services = new HashMap<>();

  private TestKit(long nativeHandle, Map<Short, UserServiceAdapter> serviceAdapters) {
    super(nativeHandle, true);
    populateServiceMap(serviceAdapters);
  }

  private static TestKit newInstance(List<Class<? extends ServiceModule>> serviceModules,
                                     EmulatedNodeType nodeType, short validatorCount,
                                     @Nullable TimeProvider timeProvider) {
    Injector frameworkInjector = Guice.createInjector(new TestKitFrameworkModule());
    return newInstanceWithInjector(serviceModules, nodeType, validatorCount, timeProvider,
        frameworkInjector);
  }

  private static TestKit newInstanceWithInjector(
      List<Class<? extends ServiceModule>> serviceModules, EmulatedNodeType nodeType,
      short validatorCount, @Nullable TimeProvider timeProvider, Injector frameworkInjector) {
    Map<Short, UserServiceAdapter> serviceAdapters = toUserServiceAdapters(
        serviceModules, frameworkInjector);
    boolean isAuditorNode = nodeType == EmulatedNodeType.AUDITOR;
    UserServiceAdapter[] userServiceAdapters = serviceAdapters.values()
        .toArray(new UserServiceAdapter[0]);
    TimeProviderAdapter timeProviderAdapter =  timeProvider == null
        ? null
        : new TimeProviderAdapter(timeProvider);
    long nativeHandle = nativeCreateTestKit(userServiceAdapters, isAuditorNode, validatorCount,
        timeProviderAdapter);
    return new TestKit(nativeHandle, serviceAdapters);
  }

  /**
   * Returns a list of user service adapters created from given service modules.
   */
  private static Map<Short, UserServiceAdapter> toUserServiceAdapters(
      List<Class<? extends ServiceModule>> serviceModules, Injector frameworkInjector) {
    List<UserServiceAdapter> services = serviceModules.stream()
        .map(s -> createUserServiceAdapter(s, frameworkInjector))
        .collect(toList());
    return Maps.uniqueIndex(services, UserServiceAdapter::getId);
  }

  /**
   * Instantiates a service given its module and wraps in a UserServiceAdapter for the native code.
   */
  private static UserServiceAdapter createUserServiceAdapter(
      Class<? extends ServiceModule> moduleClass, Injector frameworkInjector) {
    try {
      Supplier<ServiceModule> moduleSupplier = new ReflectiveModuleSupplier(moduleClass);
      ServiceModule serviceModule = moduleSupplier.get();
      Injector serviceInjector = frameworkInjector.createChildInjector(serviceModule);
      return serviceInjector.getInstance(UserServiceAdapter.class);
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("Cannot access the no-arg module constructor", e);
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("No no-arg constructor", e);
    }
  }

  private void populateServiceMap(Map<Short, UserServiceAdapter> serviceAdapters) {
    serviceAdapters.forEach((id, serviceAdapter) -> services.put(id, serviceAdapter.getService()));
  }

  /**
   * Creates a TestKit network with a single validator node for a single service.
   */
  public static TestKit forService(Class<? extends ServiceModule> serviceModule) {
    return newInstance(singletonList(serviceModule), EmulatedNodeType.VALIDATOR, (short) 1, null);
  }

  /**
   * Returns an instance of a service with the given service id and service class. Only
   * user-defined services can be requested, i.e., it is not possible to get an instance of a
   * built-in service such as the time oracle.
   *
   * @return the service instance or null if there is no service with such id
   * @throws IllegalArgumentException if the service with given id was not found or could not be
   *     cast to given class
   */
  public <T extends Service> T getService(short serviceId, Class<T> serviceClass) {
    Service service = services.get(serviceId);
    checkArgument(service != null, "Service with given id=%s was not found", serviceId);
    checkArgument(service.getClass().equals(serviceClass),
        "Service (id=%s, class=%s) cannot be cast to %s",
        serviceId, service.getClass().getCanonicalName(), serviceClass.getCanonicalName());
    return serviceClass.cast(service);
  }

  /**
   * Creates a block with the given transaction(s). Transactions are applied in the lexicographical
   * order of their hashes. In-pool transactions will be ignored.
   *
   * @return created block
   * @throws RuntimeException if transactions are malformed or don't belong to this
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
   * @throws RuntimeException if transactions are malformed or don't belong to this
   *     service
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
   * Returns a list of in-pool transactions that match the given predicate.
   */
  public List<TransactionMessage> findTransactionsInPool(Predicate<TransactionMessage> predicate) {
    return withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      MapIndex<HashCode, TransactionMessage> txMessages = blockchain.getTxMessages();
      // As only executed transactions are stored in TxResults, it wouldn't contain in-pool
      // transactions
      ProofMapIndexProxy<HashCode, TransactionResult> txResults = blockchain.getTxResults();
      List<TransactionMessage> messages = ImmutableList.copyOf(txMessages.values());
      return messages.stream()
          .filter(predicate)
          .filter(not(tx -> txResults.containsKey(tx.hash())))
          .collect(toList());
    });
  }

  /**
   * Performs a given function with a snapshot of the current database state (i.e., the one that
   * corresponds to the latest committed block). In-pool (not yet processed) transactions are also
   * accessible with it in {@linkplain Blockchain#getTxMessages() blockchain}.
   *
   * @param snapshotFunction a function to execute
   * @param <ResultT> a type the function returns
   * @return the result of applying the given function to the database state
   */
  public <ResultT> ResultT withSnapshot(Function<Snapshot, ResultT> snapshotFunction) {
    try (Cleaner cleaner = new Cleaner("TestKit#withSnapshot")) {
      long snapshotHandle = nativeCreateSnapshot(nativeHandle.get());
      Snapshot snapshot = Snapshot.newInstance(snapshotHandle, cleaner);
      return snapshotFunction.apply(snapshot);
    } catch (CloseFailuresException e) {
      throw new RuntimeException(e);
    }
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
    nativeFreeTestKit(nativeHandle.get());
  }

  private static native long nativeCreateTestKit(UserServiceAdapter[] services, boolean auditor,
      short withValidatorCount, TimeProviderAdapter timeProvider);

  private native long nativeCreateSnapshot(long nativeHandle);

  private native byte[] nativeCreateBlock(long nativeHandle);

  private native byte[] nativeCreateBlockWithTransactions(long nativeHandle, byte[][] transactions);

  private native EmulatedNode nativeGetEmulatedNode(long nativeHandle);

  private native void nativeFreeTestKit(long nativeHandle);

  /**
   * Creates a new builder for the TestKit.
   *
   * @param nodeType type of the main TestKit node - either validator or auditor. Note that
   * {@link Service#afterCommit(BlockCommittedEvent)} logic will only be called on the main TestKit
   *     node of this type
   */
  public static Builder builder(EmulatedNodeType nodeType) {
    checkNotNull(nodeType);
    return new Builder(nodeType);
  }

  /**
   * Builder for the TestKit.
   */
  public static final class Builder {

    private EmulatedNodeType nodeType;
    private short validatorCount = 1;
    private List<Class<? extends ServiceModule>> services = new ArrayList<>();
    private TimeProvider timeProvider;

    private Builder(EmulatedNodeType nodeType) {
      this.nodeType = nodeType;
    }

    /**
     * Sets number of validator nodes in the TestKit network, should be positive. Note that
     * regardless of the configured number of validators, only a single service will be
     * instantiated. Equal to one by default.
     *
     * @throws IllegalArgumentException if validatorCount is less than one
     */
    public Builder withValidators(short validatorCount) {
      checkArgument(validatorCount > 0, "TestKit network should have at least one validator node");
      this.validatorCount = validatorCount;
      return this;
    }

    /**
     * Adds a service with which the TestKit would be instantiated. Several services can be added.
     */
    public Builder withService(Class<? extends ServiceModule> serviceModule) {
      services.add(serviceModule);
      return this;
    }

    /**
     * Adds services with which the TestKit would be instantiated.
     */
    @SafeVarargs
    public final Builder withServices(Class<? extends ServiceModule> serviceModule,
                                      Class<? extends ServiceModule>... serviceModules) {
      return withServices(Lists.asList(serviceModule, serviceModules));
    }

    /**
     * Adds services with which the TestKit would be instantiated.
     */
    public Builder withServices(Iterable<Class<? extends ServiceModule>> serviceModules) {
      Iterables.addAll(services, serviceModules);
      return this;
    }

    /**
     * If called, will create a TestKit with time service enabled. The time service will use the
     * given {@linkplain TimeProvider} as a time source.
     */
    public Builder withTimeService(TimeProvider timeProvider) {
      this.timeProvider = timeProvider;
      return this;
    }

    /**
     * Creates the TestKit instance.
     */
    public TestKit build() {
      checkCorrectServiceNumber(services.size());
      return newInstance(services, nodeType, validatorCount, timeProvider);
    }

    private void checkCorrectServiceNumber(int serviceCount) {
      checkArgument(0 < serviceCount && serviceCount <= MAX_SERVICE_NUMBER,
          "Number of services must be in range [1; %s], but was %s",
          MAX_SERVICE_NUMBER, serviceCount);
    }
  }
}

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
import static com.google.common.collect.Lists.asList;
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
import com.exonum.binding.service.Service;
import com.exonum.binding.service.ServiceModule;
import com.exonum.binding.service.adapters.UserServiceAdapter;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
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
 * network, only one node will create the service instances and will execute their operations
 * (e.g., {@link Service#afterCommit(BlockCommittedEvent)} method logic).
 *
 * <p>When TestKit is created, Exonum blockchain instance is initialized, given service instances
 * are created and their
 * {@linkplain UserServiceAdapter#initialize(long)}  initialization} methods are called and
 * genesis block is committed. The
 * {@linkplain UserServiceAdapter#mountPublicApiHandler(long)} public API handlers} are created.
 *
 * @see <a href="https://exonum.com/doc/version/0.10/get-started/test-service/">TestKit documentation</a>
 */
public final class TestKit extends AbstractCloseableNativeProxy {

  @VisibleForTesting
  static final short MAX_SERVICE_NUMBER = 256;
  private static final Serializer<Block> BLOCK_SERIALIZER = BlockSerializer.INSTANCE;
  private static final Injector frameworkInjector =
      Guice.createInjector(new TestKitFrameworkModule());

  private final Map<Short, Service> services = new HashMap<>();

  private static TestKit newInstance(List<Class<? extends ServiceModule>> serviceModules,
                                     EmulatedNodeType nodeType, short validatorCount,
                                     @Nullable TimeProvider timeProvider) {
    List<UserServiceAdapter> serviceAdapters = toUserServiceAdapters(serviceModules);
    boolean isAuditorNode = nodeType == EmulatedNodeType.AUDITOR;
    UserServiceAdapter[] userServiceAdapters = serviceAdapters.toArray(new UserServiceAdapter[0]);
    // TODO: fix after native implementation
    long nativeHandle = 0L;
    //    long nativeHandle = nativeCreateTestKit(userServiceAdapters, isAuditorNode,
    //    validatorCount, timeProvider);
    return new TestKit(nativeHandle, serviceAdapters);
  }

  private TestKit(long nativeHandle, List<UserServiceAdapter> serviceAdapters) {
    super(nativeHandle, true);
    populateServiceMap(serviceAdapters);
  }

  /**
   * Returns a list of user service adapters created from given service modules.
   */
  private static List<UserServiceAdapter> toUserServiceAdapters(
      List<Class<? extends ServiceModule>> serviceModules) {
    return serviceModules.stream()
        .map(TestKit::createUserServiceAdapter)
        .collect(toList());
  }

  /**
   * Instantiates a service given its module and wraps in a UserServiceAdapter for the native code.
   */
  private static UserServiceAdapter createUserServiceAdapter(
      Class<? extends ServiceModule> moduleClass) {
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

  private void populateServiceMap(List<UserServiceAdapter> serviceAdapters) {
    for (UserServiceAdapter serviceAdapter: serviceAdapters) {
      checkForDuplicateService(serviceAdapter);
      services.put(serviceAdapter.getId(), serviceAdapter.getService());
    }
  }

  private void checkForDuplicateService(UserServiceAdapter newService) {
    short serviceId = newService.getId();
    checkArgument(!services.containsKey(serviceId),
        "Service with id %s was added to the TestKit twice: %s and %s",
        serviceId, services.get(serviceId), newService.getService());
  }

  /**
   * Creates a TestKit network with a single validator node for a single service.
   */
  public static TestKit forService(Class<? extends ServiceModule> serviceModule) {
    return newInstance(singletonList(serviceModule), EmulatedNodeType.VALIDATOR, (short) 0, null);
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
   * Creates a block with the given transaction. Transactions that are in the pool will be ignored.
   *
   * @return created block
   * @see <a href="https://exonum.com/doc/version/0.10/advanced/consensus/specification/#pool-of-unconfirmed-transactions">Pool of Unconfirmed Transactions</a>
   */
  public Block createBlockWithTransactions(TransactionMessage transaction,
                                           TransactionMessage... transactions) {
    return createBlockWithTransactions(asList(transaction, transactions));
  }

  /**
   * Creates a block with the given transactions. Transactions that are in the pool will be ignored.
   *
   * @return created block
   * @see <a href="https://exonum.com/doc/version/0.10/advanced/consensus/specification/#pool-of-unconfirmed-transactions">Pool of Unconfirmed Transactions</a>
   */
  public Block createBlockWithTransactions(Iterable<TransactionMessage> transactions) {
    List<TransactionMessage> messageList = Lists.newArrayList(transactions);
    byte[][] transactionMessagesArr = messageList.stream()
        .map(TransactionMessage::toBytes)
        .toArray(byte[][]::new);
    byte[] block = nativeCreateBlockWithTransactions(nativeHandle.get(), transactionMessagesArr);
    return BLOCK_SERIALIZER.fromBytes(block);
  }

  /**
   * Creates a block with the given transaction. Transactions that are in the pool will be ignored.
   *
   * @return created block
   * @see <a href="https://exonum.com/doc/version/0.10/advanced/consensus/specification/#pool-of-unconfirmed-transactions">Pool of Unconfirmed Transactions</a>
   */
  public Block createBlockWithTransaction(TransactionMessage transaction) {
    byte[][] transactionMessageArray = { transaction.toBytes() };
    byte[] block = nativeCreateBlockWithTransactions(nativeHandle.get(), transactionMessageArray);
    return BLOCK_SERIALIZER.fromBytes(block);
  }

  /**
   * Creates a block with all transactions in the pool.
   *
   * @return created block
   * @see <a href="https://exonum.com/doc/version/0.10/advanced/consensus/specification/#pool-of-unconfirmed-transactions">Pool of Unconfirmed Transactions</a>
   */
  public Block createBlock() {
    byte[] block = nativeCreateBlock(nativeHandle.get());
    return BLOCK_SERIALIZER.fromBytes(block);
  }

  /**
   * Returns a list of in-pool transactions that match the given predicate.
   *
   * @see <a href="https://exonum.com/doc/version/0.10/advanced/consensus/specification/#pool-of-unconfirmed-transactions">Pool of Unconfirmed Transactions</a>
   */
  public List<TransactionMessage> findTransactionsInPool(Predicate<TransactionMessage> predicate) {
    return withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      MapIndex<HashCode, TransactionMessage> txMessages = blockchain.getTxMessages();
      List<TransactionMessage> messageList = ImmutableList.copyOf(txMessages.values());
      // As only executed transactions are stored in TxResults, it wouldn't contain in-pool
      // transactions
      ProofMapIndexProxy<HashCode, TransactionResult> txResults = blockchain.getTxResults();
      return messageList.stream()
          .filter(predicate)
          // TODO: is tx.hash() correct?
          .filter(tx -> !txResults.containsKey(tx.hash()))
          .collect(toList());
    });
  }

  /**
   * Performs a given function with a snapshot of the current database state.
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
   * Returns the emulated TestKit node context.
   */
  public EmulatedNode getEmulatedNode() {
    return nativeGetEmulatedNode(nativeHandle.get());
  }

  @Override
  protected void disposeInternal() {
    nativeFreeTestKit(nativeHandle.get());
  }

  private native long nativeCreateTestKit(UserServiceAdapter[] services, boolean auditor,
      short withValidatorCount, TimeProvider timeProvider);

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
    private short validatorCount;
    private List<Class<? extends ServiceModule>> services = new ArrayList<>();
    private TimeProvider timeProvider;

    private Builder(EmulatedNodeType nodeType) {
      // TestKit network should have at least one validator node
      if (nodeType == EmulatedNodeType.AUDITOR) {
        validatorCount = 1;
      }
      this.nodeType = nodeType;
    }

    /**
     * Sets number of additional validator nodes in the TestKit network. Note that
     * regardless of the configured number of validators, only a single service will be
     * instantiated.
     */
    public final Builder withValidators(short validatorCount) {
      this.validatorCount = validatorCount;
      return this;
    }

    /**
     * Adds a service with which the TestKit would be instantiated. Several services can be added.
     */
    public final Builder withService(Class<? extends ServiceModule> serviceModule) {
      services.add(serviceModule);
      return this;
    }

    /**
     * Adds services with which the TestKit would be instantiated.
     */
    @SafeVarargs
    public final Builder withServices(Class<? extends ServiceModule> serviceModule,
                                      Class<? extends ServiceModule>... serviceModules) {
      return withServices(asList(serviceModule, serviceModules));
    }

    /**
     * Adds services with which the TestKit would be instantiated.
     */
    public final Builder withServices(Iterable<Class<? extends ServiceModule>> serviceModules) {
      Iterables.addAll(services, serviceModules);
      return this;
    }

    /**
     * If called, will create a TestKit with time service enabled. The time service will use the
     * given {@linkplain TimeProvider} as a time source.
     */
    public final Builder withTimeService(TimeProvider timeProvider) {
      this.timeProvider = timeProvider;
      return this;
    }

    /**
     * Creates the TestKit instance.
     */
    public final TestKit build() {
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

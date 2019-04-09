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

import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.runtime.ReflectiveModuleSupplier;
import com.exonum.binding.service.BlockCommittedEvent;
import com.exonum.binding.service.Service;
import com.exonum.binding.service.ServiceModule;
import com.exonum.binding.service.adapters.UserServiceAdapter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * TestKit for testing blockchain services. It offers simple network configuration emulation
 * (with no real network setup). Although it is possible to add several validator nodes to this
 * network, only one node will create the service instances and will execute their operations
 * (e.g., {@link Service#afterCommit(BlockCommittedEvent)} method logic).
 *
 * <p>When TestKit is created, Exonum blockchain instance is initialized - service instances are
 * {@linkplain UserServiceAdapter#initialize(long)} initialized} and genesis block is committed.
 * Then the {@linkplain UserServiceAdapter#mountPublicApiHandler(long)} public API handlers} are
 * created.
 *
 * @see <a href="https://exonum.com/doc/version/0.10/get-started/test-service/">TestKit documentation</a>
 */
public final class TestKit {

  @VisibleForTesting
  static final short MAX_SERVICE_NUMBER = 256;
  private final Injector frameworkInjector = Guice.createInjector(new TestKitFrameworkModule());

  private final NativeHandle nativeHandle;
  private final Map<Short, Service> services = new HashMap<>();

  private TestKit(List<Class<? extends ServiceModule>> serviceModules, EmulatedNodeType nodeType,
                  short validatorCount, @Nullable TimeProvider timeProvider) {
    List<UserServiceAdapter> serviceAdapters = toUserServiceAdapters(serviceModules);
    populateServiceMap(serviceAdapters);
    boolean isAuditorNode = nodeType == EmulatedNodeType.AUDITOR;
    UserServiceAdapter[] userServiceAdapters = serviceAdapters.toArray(new UserServiceAdapter[0]);
    // TODO: fix after native implementation
    nativeHandle = null;
    //  nativeHandle = new NativeHandle(
    //      nativeCreateTestKit(userServiceAdapters, isAuditorNode, validatorCount, timeProvider));
  }

  /**
   * Returns a list of user service adapters created from given service modules.
   */
  private List<UserServiceAdapter> toUserServiceAdapters(
      List<Class<? extends ServiceModule>> serviceModules) {
    return serviceModules.stream()
        .map(this::createUserServiceAdapter)
        .collect(toList());
  }

  /**
   * Instantiates a service given its module and wraps in a UserServiceAdapter for the native code.
   */
  private UserServiceAdapter createUserServiceAdapter(Class<? extends ServiceModule> moduleClass) {
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
    return new TestKit(singletonList(serviceModule), EmulatedNodeType.VALIDATOR, (short) 0, null);
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

  private native long nativeCreateTestKit(UserServiceAdapter[] services, boolean auditor,
      short withValidatorCount, TimeProvider timeProvider);

  private native long nativeCreateSnapshot(long nativeHandle);

  private native byte[] nativeCreateBlock(long nativeHandle);

  private native byte[] nativeCreateBlockWithTransactions(long nativeHandle, byte[][] transactions);

  private native EmulatedNode nativeGetEmulatedNode(long nativeHandle);

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
    public Builder withValidators(short validatorCount) {
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
      return withServices(asList(serviceModule, serviceModules));
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
      return new TestKit(services, nodeType, validatorCount, timeProvider);
    }

    private void checkCorrectServiceNumber(int serviceCount) {
      checkArgument(0 < serviceCount && serviceCount <= MAX_SERVICE_NUMBER,
          "Number of services must be in range [1; %s], but was %s",
          MAX_SERVICE_NUMBER, serviceCount);
    }
  }
}

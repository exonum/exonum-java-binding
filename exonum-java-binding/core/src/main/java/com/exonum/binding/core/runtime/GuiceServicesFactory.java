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

import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.service.ServiceModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.function.Supplier;

/**
 * The default service factory. Although the naming choice might imply non-Guice factories are
 * possible, it is not true — see the {@link ServicesFactory} javadoc.
 */
final class GuiceServicesFactory implements ServicesFactory {

  private final Injector frameworkInjector;

  /**
   * Creates a new factory of services with the given framework injector.
   *
   * @param frameworkInjector the framework injector providing system-wide bindings to services
   */
  @Inject
  GuiceServicesFactory(Injector frameworkInjector) {
    this.frameworkInjector = frameworkInjector;
  }

  @Override
  public ServiceWrapper createService(
      LoadedServiceDefinition definition, ServiceInstanceSpec instanceSpec, Node node) {
    // Take the user-supplied module configuring service bindings
    Supplier<ServiceModule> serviceModuleSupplier = definition.getModuleSupplier();
    Module serviceModule = serviceModuleSupplier.get();
    // Create a framework-supplied module with per-service bindings
    Module serviceFrameworkModule = new ServiceFrameworkModule(instanceSpec, node);
    // Create a new service
    // todo: [ECR-3433] Reconsider the relationships between the framework injector and the child.
    //   Currently the child injector sees everything from the parent, but it does not
    //   seem to need that, the service needs only a well-defined subset of dependencies.
    Injector serviceInjector =
        frameworkInjector.createChildInjector(serviceModule, serviceFrameworkModule);
    return serviceInjector.getInstance(ServiceWrapper.class);
  }
}

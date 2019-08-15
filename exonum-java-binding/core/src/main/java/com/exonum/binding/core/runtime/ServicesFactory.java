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

/**
 * A factory of Exonum services. It takes the service definition, the instance parameters,
 * and produces a service.
 *
 * <p>This factory primarily exists to make the runtime testing easier. It does not,
 * in its present form, abstract the instantiation mechanism (Guice), as it requires the service
 * definition with a Guice module providing service bindings. It is a deliberate design decision
 * to not over-abstract the interaction between the service loader producing
 * the service definition; the factory consuming it to make a service instance;
 * and their client — the runtime.
 */
@FunctionalInterface
interface ServicesFactory {

  /**
   * Creates a service from its definition with the given instance parameters.
   *
   * @param definition the loaded service definition
   * @param instanceSpec the service instance specification, including its parameters
   */
  ServiceWrapper createService(LoadedServiceDefinition definition,
      ServiceInstanceSpec instanceSpec);
}

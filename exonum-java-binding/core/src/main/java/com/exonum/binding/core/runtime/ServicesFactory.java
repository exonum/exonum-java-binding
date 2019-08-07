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

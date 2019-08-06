package com.exonum.binding.core.runtime;

import com.google.inject.AbstractModule;

/**
 * A framework module providing per-service bindings. These bindings are supplied
 * by the framework.
 */
class ServiceFrameworkModule extends AbstractModule {

  private final ServiceInstanceSpec instanceSpec;

  ServiceFrameworkModule(ServiceInstanceSpec instanceSpec) {
    this.instanceSpec = instanceSpec;
  }

  @Override
  protected void configure() {
    // todo: consider named bindings for the name and id — they will require publicly
    //   accessible key names.
    bind(ServiceInstanceSpec.class).toInstance(instanceSpec);
  }
}

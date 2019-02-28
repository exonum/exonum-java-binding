package com.exonum.binding.runtime;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class DefaultLoadedServiceDefinition implements LoadedServiceDefinition {

  @Override
  public abstract ServiceId getId();

  static DefaultLoadedServiceDefinition newInstance(ServiceId serviceId) {
    return new AutoValue_DefaultLoadedServiceDefinition(serviceId);
  }
}

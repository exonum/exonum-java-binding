package com.exonum.binding.runtime;

import com.exonum.binding.service.ServiceModule;
import com.google.auto.value.AutoValue;
import java.util.function.Supplier;

@AutoValue
abstract class DefaultLoadedServiceDefinition implements LoadedServiceDefinition {

  @Override
  public abstract ServiceId getId();

  @Override
  public abstract Supplier<? extends ServiceModule> getModuleSupplier();

  static DefaultLoadedServiceDefinition newInstance(ServiceId serviceId,
      Supplier<? extends ServiceModule> serviceModuleSupplier) {
    return new AutoValue_DefaultLoadedServiceDefinition(serviceId, serviceModuleSupplier);
  }
}

package com.exonum.binding.runtime;

import com.exonum.binding.service.ServiceModule;
import com.google.auto.value.AutoValue;
import java.util.function.Supplier;


/**
 * A complete definition of a loaded service that allows the framework to identify and instantiate
 * service instances.
 */
@AutoValue
abstract class LoadedServiceDefinition {

  /**
   * Returns the unique identifier of the service.
   */
  public abstract ServiceId getId();

  /**
   * Returns a supplier of {@linkplain ServiceModule service modules} configuring their bindings.
   * The supplier will always return the same module corresponding to this service, but not
   * necessarily the same instance.
   */
  public abstract Supplier<ServiceModule> getModuleSupplier();

  static LoadedServiceDefinition newInstance(ServiceId serviceId,
      Supplier<ServiceModule> serviceModuleSupplier) {
    return new AutoValue_LoadedServiceDefinition(serviceId, serviceModuleSupplier);
  }
}

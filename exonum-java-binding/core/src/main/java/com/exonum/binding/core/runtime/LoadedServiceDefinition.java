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

import com.exonum.binding.common.runtime.ServiceArtifactId;
import com.exonum.binding.core.service.ServiceModule;
import com.google.auto.value.AutoValue;
import java.util.function.Supplier;


/**
 * A complete definition of a loaded service that allows the framework to identify and instantiate
 * service instances.
 */
@AutoValue
abstract class LoadedServiceDefinition {

  /**
   * Returns the unique identifier of the service artifact.
   */
  public abstract ServiceArtifactId getId();

  /**
   * Returns a supplier of {@linkplain ServiceModule service modules} configuring their bindings.
   * The supplier will always return the same module corresponding to this service, but not
   * necessarily the same instance.
   */
  public abstract Supplier<ServiceModule> getModuleSupplier();

  static LoadedServiceDefinition newInstance(ServiceArtifactId artifactId,
      Supplier<ServiceModule> serviceModuleSupplier) {
    return new AutoValue_LoadedServiceDefinition(artifactId, serviceModuleSupplier);
  }
}

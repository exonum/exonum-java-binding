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

package com.exonum.binding.runtime;

import java.net.URI;
import java.util.Optional;

/**
 * A dynamic loader of Exonum service artifacts. It allows to load and unload service artifacts and
 * keeps track of already loaded services.
 */
interface ServiceLoader {

  /**
   * Loads the service artifact. The loading also involves verification of the artifact, e.g.,
   * checking that it has correct metadata. The actual verifications are implementation-specific.
   *
   * <p>Upon successful completion of this method, the service is considered as loaded
   * and instances of this service can be created.
   *
   * @param artifactLocation the location of the service artifact (a file, network resource, etc.)
   * @return a complete definition of a service from the artifact
   * @throws ServiceLoadingException in case the artifact cannot be loaded: not found or
   *     does not pass verification
   */
  LoadedServiceDefinition loadService(URI artifactLocation) throws ServiceLoadingException;

  /**
   * Returns a loaded service with the given id; or {@link Optional#empty()} if there is no such.
   * @param serviceId the identifier of the service
   */
  Optional<LoadedServiceDefinition> findService(ServiceId serviceId);

  /**
   * Unloads a previously {@linkplain #loadService(URI) loaded} service. The clients <b>must
   * not</b> unload the service while it is in use (there are active instances of it).
   *
   * <p>Once the service is unloaded, its definition shall no longer be used to create any new
   * service instances.
   *
   * @param serviceId the identifier of the loaded service
   * @throws IllegalStateException if the service identified by the given id is not currently loaded
   *     TODO: Shall we throw or make a no-op?
   */
  void unloadService(ServiceId serviceId);
}

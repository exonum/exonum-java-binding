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

import java.nio.file.Path;
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
   * @param artifactPath the location of the service artifact file
   * @return a complete definition of a service from the artifact
   * @throws ServiceLoadingException in case the artifact cannot be loaded: not found or
   *     does not pass verification
   */
  LoadedServiceDefinition loadService(Path artifactPath) throws ServiceLoadingException;

  /**
   * Returns a loaded service with the given id; or {@link Optional#empty()} if there is no such.
   * @param artifactId the identifier of the service artifact
   */
  Optional<LoadedServiceDefinition> findService(ServiceArtifactId artifactId);

  /**
   * Unloads a previously {@linkplain #loadService(Path) loaded} service. The clients <b>must
   * not</b> unload the service while it is in use (there are active instances of it).
   *
   * <p>Once the service is unloaded, its definition shall no longer be used to create any new
   * service instances.
   *
   * @param artifactId the identifier of the loaded service artifact
   * @throws IllegalStateException if the service identified by the given id is not currently loaded
   */
  void unloadService(ServiceArtifactId artifactId);

  /**
   * Unloads all previously loaded services. The clients <b>must not</b> unload the services
   * while any of them are in use (there are active instances of them).
   *
   * <p>This method will attempt to unload each service, and communicate any exceptions
   * afterwards.
   *
   * @throws IllegalStateException if any service failed to unload, the exceptions they
   *     had thrown will be in the list of suppressed exceptions
   */
  void unloadAll();
}

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

package com.exonum.binding.fakes;

import com.exonum.binding.core.runtime.ServiceArtifactId;
import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.runtime.ServiceRuntime;
import com.exonum.binding.core.util.LibraryLoader;
import com.exonum.binding.fakes.services.ServiceArtifacts;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Provides methods to create mocks and test fakes of Service and Transaction adapters.
 *
 * <p>This class is a one stop place to </p>
 */
@SuppressWarnings({"unused", "WeakerAccess"}) // Used in native code
public final class NativeFacade {

  static {
    // Load the native library early when this class is used in native integration tests.
    LibraryLoader.load();
  }

  /**
   * Writes a valid service artifact to the specified location. A valid service artifact
   * can be loaded by the {@link ServiceRuntime} and
   * the service can be instantiated.
   * @param artifactId the id of the artifact
   * @param path a path to write the artifact to
   * @throws IOException if it is unable to write the JAR to the given location
   */
  public static void createValidServiceArtifact(String artifactId, String path) throws IOException {
    ServiceArtifacts.createValidArtifact(ServiceArtifactId.parseFrom(artifactId), Paths.get(path));
  }

  /**
   * Writes a service artifact that cannot be loaded. Such artifact will cause an exception
   * during an attempt
   * to {@linkplain ServiceRuntime#deployArtifact(ServiceArtifactId, String) load} it.
   * @param artifactId the id of the artifact
   * @param path a path to write the artifact to
   * @throws IOException if it is unable to write the JAR to the given location
   */
  public static void createUnloadableServiceArtifact(String artifactId, String path)
      throws IOException {
    ServiceArtifacts.createUnloadableArtifact(artifactId, Paths.get(path));
  }

  /**
   * Writes a service artifact that can be loaded, but with a service that cannot be
   * {@linkplain ServiceRuntime#createService(ServiceInstanceSpec) instantiated}.
   * @param artifactId the id of the artifact
   * @param path a path to write the artifact to
   * @throws IOException if it is unable to write the JAR to the given location
   */
  public static void createServiceArtifactWithNonInstantiableService(String artifactId, String path)
      throws IOException {
    ServiceArtifacts.createWithUninstantiableService(ServiceArtifactId.parseFrom(artifactId),
        Paths.get(path));
  }

  private NativeFacade() {}
}

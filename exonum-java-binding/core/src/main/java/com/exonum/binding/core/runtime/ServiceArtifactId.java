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

import static com.exonum.binding.core.runtime.RuntimeId.JAVA;
import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Integer.parseInt;
import static org.apache.logging.log4j.util.Strings.isNotBlank;

import com.exonum.messages.core.runtime.Base.ArtifactId;
import com.google.auto.value.AutoValue;

/**
 * A service artifact identifier. It consists of the runtime id in which the service shall be
 * deployed, the service artifact name and its version.
 *
 * <p>The extensions of this class must be immutable and hence thread-safe.
 */
@AutoValue
public abstract class ServiceArtifactId {

  private static final String DELIMITER = ":";
  private static final int KEEP_EMPTY = -1;

  /**
   * Returns the runtime id in which the service shall be deployed.
   */
  public abstract int getRuntimeId();

  /**
   * Returns the artifact name of this service.
   * The name of Java artifacts usually (but not necessary) contains the two coordinates:
   * groupId and artifactId separated by "/" (e.g., "com.acme/land-registry").
   */
  public abstract String getName();

  /**
   * Returns the artifact version of this service (e.g., "1.2.0").
   */
  public abstract String getVersion();

  /**
   * Parses a service id in format "runtimeId:serviceName:version" as {@link #toString()} produces.
   *
   * @param serviceArtifactId a string in format "runtimeId:serviceName:version". Whitespace
   *     characters, including preceding and trailing, are not allowed
   * @return a ServiceArtifactId with the given coordinates
   * @throws IllegalArgumentException if the format is not correct
   */
  public static ServiceArtifactId parseFrom(String serviceArtifactId) {
    JavaArtifactUtils.checkNoForbiddenChars(serviceArtifactId);
    String[] coordinates = serviceArtifactId.split(DELIMITER, KEEP_EMPTY);
    checkArgument(coordinates.length == 3,
        "Invalid artifact id (%s), must have 'runtimeId:artifactName:version' format",
        serviceArtifactId);
    int runtimeId = parseInt(coordinates[0]);
    String name = coordinates[1];
    String version = coordinates[2];
    return valueOf(runtimeId, name, version);
  }

  /**
   * Creates a new service artifact id of a Java artifact.
   *
   * @param name the name of the service; must not be blank
   */
  public static ServiceArtifactId newJavaId(String name, String version) {
    return valueOf(JAVA.getId(), name, version);
  }

  /**
   * Creates a new service artifact id.
   *
   * @param runtimeId the runtime id in which the service shall be deployed
   * @param name the name of the service; must not be blank
   */
  public static ServiceArtifactId valueOf(int runtimeId, String name, String version) {
    checkArgument(isNotBlank(name), "name is blank: '%s'", name);
    checkArgument(isNotBlank(version), "version is blank: '%s'", version);
    return new AutoValue_ServiceArtifactId(runtimeId, name, version);
  }

  /**
   * Creates a new service artifact from the given artifact id message.
   */
  public static ServiceArtifactId fromProto(ArtifactId artifactId) {
    return valueOf(artifactId.getRuntimeId(), artifactId.getName(), artifactId.getVersion());
  }

  /**
   * Returns an artifact id in the following format: "runtimeId:serviceName:version".
   */
  @Override
  public final String toString() {
    return getRuntimeId() + DELIMITER + getName() + DELIMITER + getVersion();
  }
}

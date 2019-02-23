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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.auto.value.AutoValue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A service artifact identifier. It consist of the three components that usually identify any
 * Java artifact: groupId, artifactId and version.
 *
 * <p>The extensions of this class must be immutable and hence thread-safe.
 */
@AutoValue
public abstract class ServiceId {

  private static final String DELIMITER = ":";
  private static final Pattern FORBIDDEN_CHARS_PATTERN = Pattern.compile("[\\s:]");
  private static final int KEEP_EMPTY = -1;

  /**
   * Returns the group id of this service (e.g., "com.acme").
   */
  public abstract String getGroupId();

  /**
   * Returns the artifact id of this service (e.g., "land-registry"), aka service name.
   */
  public abstract String getArtifactId();

  /**
   * Returns the version of this service (e.g., "1.2.0").
   */
  public abstract String getVersion();

  /**
   * Parses a service id in format "groupId:artifactId:version" as {@link #toString()} produces.
   *
   * @param serviceId a string in format "groupId:artifactId:version". Whitespace characters,
   *     including preceding and trailing, are not allowed
   * @return a ServiceId with the given components
   * @throws IllegalArgumentException if the format is not correct
   */
  public static ServiceId parseFrom(String serviceId) {
    String[] components = serviceId.split(DELIMITER, KEEP_EMPTY);
    checkArgument(components.length == 3, "Invalid serviceId: %s (%s components)",
        serviceId, components.length);
    String groupId = components[0];
    String artifactId = components[1];
    String version = components[2];
    return of(groupId, artifactId, version);
  }

  /**
   * Creates a new service id of the given components.
   *
   * @throws IllegalArgumentException if any component contains whitespace characters
   */
  public static ServiceId of(String groupId, String artifactId, String version) {
    return new AutoValue_ServiceId(checkNoForbiddenChars(groupId),
        checkNoForbiddenChars(artifactId),
        checkNoForbiddenChars(version));
  }

  private static String checkNoForbiddenChars(String s) {
    Matcher matcher = FORBIDDEN_CHARS_PATTERN.matcher(s);

    if (matcher.find()) {
      throw new IllegalArgumentException(String.format("'%s' must not have any forbidden "
          + "characters, but there is at index %d", s, matcher.start()));
    }
    return s;
  }

  /**
   * Returns a service id in the following format: "groupId:artifactId:version".
   */
  @Override
  public final String toString() {
    return getGroupId() + DELIMITER + getArtifactId() + DELIMITER + getVersion();
  }
}

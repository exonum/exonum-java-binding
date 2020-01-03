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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaArtifactNames {

  private static final String DELIMITER = ":";
  private static final Pattern FORBIDDEN_CHARS_PATTERN = Pattern.compile("[\\s:]");
  private static final int KEEP_EMPTY = -1;

  /**
   * Checks that a Java artifact name is in format "groupId/artifactId:version".
   *
   * @param name a Java artifact name
   * @return an array with two elements: artifact name and artifact version
   * @throws IllegalArgumentException if the name format is not correct
   */
  static String[] checkPluginArtifact(String name) {
    String[] coordinates = name.split(DELIMITER, KEEP_EMPTY);
    checkArgument(coordinates.length == 2,
        "Invalid artifact name (%s), must have 'groupId/artifactId:version' format",
        name);
    for (String c : coordinates) {
      checkNoForbiddenChars(c);
    }
    return coordinates;
  }

  /**
   * Returns plugin artifact id for the given service artifact.
   */
  static String getPluginArtifactId(ServiceArtifactId artifactId) {
    return artifactId.getName() + DELIMITER + artifactId.getVersion();
  }


  private static void checkNoForbiddenChars(String s) {
    Matcher matcher = FORBIDDEN_CHARS_PATTERN.matcher(s);

    if (matcher.find()) {
      throw new IllegalArgumentException(String.format("'%s' must not have any forbidden "
          + "characters, but there is '%s' at index %d", s, matcher.group(), matcher.start()));
    }
  }

  private JavaArtifactNames() {}
}

/*
 * Copyright 2020 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.common.runtime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaArtifactUtils {
  private static final Pattern FORBIDDEN_CHARS_PATTERN = Pattern.compile("[\\s]");

  /**
   * Validates the string representation of the artifact id for forbidden characters.
   * @throws IllegalArgumentException if any forbidden characters found
   */
  static void checkNoForbiddenChars(String artifactId) {
    Matcher matcher = FORBIDDEN_CHARS_PATTERN.matcher(artifactId);
    if (matcher.find()) {
      throw new IllegalArgumentException(String.format("'%s' must not have any forbidden "
              + "characters, but there is '%s' at index %d",
          artifactId, matcher.group(), matcher.start()));
    }
  }

  private JavaArtifactUtils() {
  }
}

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

package com.exonum.binding.test;

import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.core.runtime.ServiceArtifactId;
import com.google.common.base.Strings;
import java.nio.file.Path;
import java.nio.file.Paths;

final class TestArtifactInfo {

  static final Path ARTIFACT_DIR = Paths.get(getRequiredProperty("it.artifactsDir"));
  private static final String ARTIFACT_NAME = getRequiredProperty("it.artifactName");
  static final ServiceArtifactId ARTIFACT_ID = ServiceArtifactId.newJavaId(ARTIFACT_NAME);
  static final String ARTIFACT_FILENAME = getRequiredProperty("it.artifactFilename");

  private static String getRequiredProperty(String key) {
    String property = System.getProperty(key);
    checkState(!Strings.isNullOrEmpty(property),
        "Absent property: %s=%s", key, property);
    return property;
  }

  private TestArtifactInfo() {}
}

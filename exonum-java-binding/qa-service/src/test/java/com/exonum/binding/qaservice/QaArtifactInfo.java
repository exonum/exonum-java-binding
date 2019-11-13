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

package com.exonum.binding.qaservice;

import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.core.runtime.ServiceArtifactId;
import com.exonum.binding.qaservice.Config.InitialConfiguration;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKit.Builder;
import com.exonum.binding.testkit.TimeProvider;
import com.google.common.base.Strings;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Properties about the qa service artifact.
 */
public final class QaArtifactInfo {

  public static final Path ARTIFACT_DIR = Paths.get(getRequiredProperty("it.artifactsDir"));
  private static final String ARTIFACT_NAME = getRequiredProperty("it.artifactName");
  public static final ServiceArtifactId ARTIFACT_ID = ServiceArtifactId.newJavaId(ARTIFACT_NAME);
  public static final String ARTIFACT_FILENAME = getRequiredProperty("it.artifactFilename");
  public static final String TIME_SERVICE_NAME = "time";
  public static final int TIME_SERVICE_ID = 100;
  public static final String QA_SERVICE_NAME = "qa";
  public static final int QA_SERVICE_ID = 101;
  public static final InitialConfiguration QA_SERVICE_INITIAL_CONFIG =
      InitialConfiguration.newBuilder()
          .setTimeOracleName(TIME_SERVICE_NAME)
          .build();

  private static String getRequiredProperty(String key) {
    String property = System.getProperty(key);
    checkState(!Strings.isNullOrEmpty(property),
        "Absent property: %s=%s", key, property);
    return property;
  }

  /**
   * Creates a testkit builder with QA service and time service with the instance parameters
   * as specified in the constants. Time service will use the system clock.
   */
  public static Builder createQaServiceTestkit() {
    return createQaServiceTestkit(TimeProvider.systemTime());
  }

  /**
   * Creates a testkit builder with QA service and time service with the instance parameters
   * as specified in the constants.
   * @param timeProvider the time provider to use in the time service
   */
  public static Builder createQaServiceTestkit(TimeProvider timeProvider) {
    return TestKit.builder()
        .withArtifactsDirectory(ARTIFACT_DIR)
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, QA_SERVICE_NAME, QA_SERVICE_ID, QA_SERVICE_INITIAL_CONFIG)
        .withTimeService(TIME_SERVICE_NAME, TIME_SERVICE_ID, timeProvider);
  }

  private QaArtifactInfo() {}
}

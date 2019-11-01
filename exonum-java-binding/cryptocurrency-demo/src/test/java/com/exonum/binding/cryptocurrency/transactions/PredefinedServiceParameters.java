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

package com.exonum.binding.cryptocurrency.transactions;

import com.exonum.binding.core.runtime.ServiceArtifactId;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Predefined service parameters used by TestKit in integration tests.
 */
public final class PredefinedServiceParameters {
  public static final String ARTIFACT_FILENAME = System.getProperty("it.artifactFilename");
  public static final ServiceArtifactId ARTIFACT_ID =
      ServiceArtifactId.newJavaId(System.getProperty("it.artifactId"));
  public static final String SERVICE_NAME = System.getProperty("it.serviceName");
  public static final int SERVICE_ID = Integer.parseInt(System.getProperty("it.serviceId"));
  public static Path artifactsDirectory = Paths.get(System.getProperty("it.artifactsDirectory"));

  private PredefinedServiceParameters() {}
}

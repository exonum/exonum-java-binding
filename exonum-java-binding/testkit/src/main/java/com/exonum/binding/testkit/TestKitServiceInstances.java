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

package com.exonum.binding.testkit;

import com.google.auto.value.AutoValue;

/**
 * Specifications of service instances to be deployed and created by TestKit.
 */
@AutoValue
abstract class TestKitServiceInstances {

  /**
   * Returns the service artifact id.
   */
  abstract String getArtifactId();

  /**
   * Returns a filename of the service artifact.
   */
  abstract String getArtifactFilename();

  /**
   * Returns a service instance specification - its service name, service id and configuration.
   */
  abstract ServiceSpec[] getServiceSpecs();

  // TODO: AutoValue doesn't support array-valued properties unless it is a primitive
  //  array - should we use a Collection here or don't use AutoValue for this class?
  static TestKitServiceInstances newInstance(
      String artifactId, String artifactFilename, ServiceSpec[] serviceSpecs) {
    return new AutoValue_TestKitServiceInstances(artifactId, artifactFilename, serviceSpecs);
  }
}

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

/**
 * Specifications of service instances to be deployed and created by TestKit.
 */
@SuppressWarnings("unused") // Native API
class TestKitServiceInstances {

  private String artifactId;
  private String artifactFilename;
  private ServiceSpec[] serviceSpecs;

  public TestKitServiceInstances(String artifactId, String artifactFilename, ServiceSpec[] serviceSpecs) {
    this.artifactId = artifactId;
    this.artifactFilename = artifactFilename;
    this.serviceSpecs = serviceSpecs;
  }

  /**
   * Returns the service artifact id.
   */
  public String getArtifactId() {
    return artifactId;
  }

  /**
   * Returns a filename of the service artifact.
   */
  public String getArtifactFilename() {
    return artifactFilename;
  }

  /**
   * Returns a service instance specification - its service name, service id and configuration.
   */
  public ServiceSpec[] getServiceSpecs() {
    return serviceSpecs;
  }
}

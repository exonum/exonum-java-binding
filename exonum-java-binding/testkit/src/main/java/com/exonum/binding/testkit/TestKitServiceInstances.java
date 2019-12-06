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
@SuppressWarnings({"unused", "WeakerAccess"}) // Native API
class TestKitServiceInstances {

  final String artifactId;
  final byte[] deployArguments;
  final ServiceSpec[] serviceSpecs;

  TestKitServiceInstances(String artifactId, byte[] deployArguments, ServiceSpec[] serviceSpecs) {
    this.artifactId = artifactId;
    this.deployArguments = deployArguments;
    this.serviceSpecs = serviceSpecs;
  }
}

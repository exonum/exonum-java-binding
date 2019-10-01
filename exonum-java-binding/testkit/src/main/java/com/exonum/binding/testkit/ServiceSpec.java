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
 * A specification of a service instance used by TestKit for service creation.
 */
@AutoValue
abstract class ServiceSpec {

  /**
   * Returns the name of the service instance. It serves as the primary identifier of this service
   * in most operations.
   */
  abstract String getServiceName();

  /**
   * Returns the numeric id of the service instance.
   */
  abstract int getServiceId();

  /**
   * Returns the configuration of the service instance.
   */
  abstract byte[] getConfiguration();

  static ServiceSpec newInstance(String name, int id, byte[] configuration) {
    return new AutoValue_ServiceSpec(name, id, configuration);
  }
}

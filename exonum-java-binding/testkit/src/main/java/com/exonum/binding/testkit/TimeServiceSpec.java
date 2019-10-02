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
 * A specification of a time service instance used by TestKit for service creation.
 */
@SuppressWarnings("unused") // Native API
@AutoValue
abstract class TimeServiceSpec {

  /**
   * Returns {@linkplain TimeProviderAdapter} used by time service as a time source.
   */
  abstract TimeProviderAdapter getTimeProvider();

  /**
   * Returns the name of this time service instance.
   */
  abstract String getServiceName();

  /**
   * Returns the numeric id of this time service instance.
   */
  abstract int getServiceId();

  static TimeServiceSpec newInstance(TimeProviderAdapter timeProvider, String serviceName, int serviceId) {
    return new AutoValue_TimeServiceSpec(timeProvider, serviceName, serviceId);
  }
}

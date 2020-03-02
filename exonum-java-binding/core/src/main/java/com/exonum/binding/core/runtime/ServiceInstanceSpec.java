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

import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.common.runtime.ServiceArtifactId;
import com.google.auto.value.AutoValue;

/**
 * A specification of a service instance.
 */
@AutoValue
public abstract class ServiceInstanceSpec {

  /**
   * Returns the name of the service instance. It serves as the primary identifier of this service
   * in most operations. It is assigned by the network administrators.
   */
  public abstract String getName();

  /**
   * Returns the numeric id of the service instance. Exonum assigns it to the service
   * on instantiation. It is mainly used to route the transaction messages belonging
   * to this instance.
   *
   * @see TransactionMessage#getServiceId()
   */
  public abstract int getId();

  /**
   * Returns the service artifact id.
   */
  public abstract ServiceArtifactId getArtifactId();

  public static ServiceInstanceSpec newInstance(String name, int id, ServiceArtifactId artifactId) {
    return new AutoValue_ServiceInstanceSpec(name, id, artifactId);
  }
}

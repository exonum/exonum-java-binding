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

package com.exonum.binding.core.service;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.storage.database.AbstractAccess;

/**
 * A base class for user services.
 */
public abstract class AbstractService implements Service {

  private final ServiceInstanceSpec instanceSpec;

  protected AbstractService(ServiceInstanceSpec instanceSpec) {
    this.instanceSpec = checkNotNull(instanceSpec);
  }

  /**
   * Returns the name of the service instance.
   * @see ServiceInstanceSpec#getName()
   */
  protected final String getName() {
    return instanceSpec.getName();
  }

  /**
   * Returns the numeric id of the service instance.
   * @see ServiceInstanceSpec#getId()
   */
  protected final int getId() {
    return instanceSpec.getId();
  }

  /**
   * Returns this service instance specification.
   */
  protected final ServiceInstanceSpec getInstanceSpec() {
    return instanceSpec;
  }

  /**
   * Creates a data schema of this service.
   *
   * @param access a database access
   * @return a data schema of the service
   */
  protected abstract Schema createDataSchema(AbstractAccess access);
}

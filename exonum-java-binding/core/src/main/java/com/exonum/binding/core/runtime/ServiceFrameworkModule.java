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

import com.google.inject.AbstractModule;

/**
 * A framework module providing per-service bindings. These bindings are supplied
 * by the framework.
 */
class ServiceFrameworkModule extends AbstractModule {

  private final ServiceInstanceSpec instanceSpec;

  ServiceFrameworkModule(ServiceInstanceSpec instanceSpec) {
    this.instanceSpec = instanceSpec;
  }

  @Override
  protected void configure() {
    // todo: consider named bindings for the name and id — they will require publicly
    //   accessible key names.
    bind(ServiceInstanceSpec.class).toInstance(instanceSpec);
  }
}

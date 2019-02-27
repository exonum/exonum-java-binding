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

package com.exonum.binding.runtime;

/**
 * A complete definition of a loaded service that allows the framework to identify
 * and instantiate service instances.
 */
// TODO: What if we define an alternative name for "service artifact" so that the "service" term
//    is not overloaded? Currently "service"-as artifact might be confused with
//    "service"-as instance of Service (or "service instance")?
//    - Bundle (as in OSGi — short and not currently used)?
//    - Plugin (I don't think it works in our case)?
interface LoadedServiceDefinition {

  /**
   * Returns the unique identifier of the service.
   */
  ServiceId getId();


  // todo: Implement
}

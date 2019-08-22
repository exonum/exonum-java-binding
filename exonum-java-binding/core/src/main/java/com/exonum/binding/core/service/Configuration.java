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

import com.exonum.binding.core.storage.database.Fork;
import com.google.protobuf.Message;

/**
 * Configuration parameters of Exonum service. Network administrators agree on and pass
 * the configuration parameters as a service-specific protobuf message in the transactions
 * which add that service instance to the network. After Exonum starts the service, it
 * {@linkplain Service#configure(Fork, Configuration) passes the configuration parameters}
 * to the newly created service instance.
 *
 * <p>Service reconfiguration is not currently supported, but will be added soon.
 *
 * @see Service#configure(Fork, Configuration)
 */
public interface Configuration {

  /**
   * Returns the configuration parameters as a Protocol Buffers message.
   *
   * @param parametersType the type of a Protocol Buffers message in which the service configuration
   *     parameters are recorded in transactions starting the service instance
   * @throws IllegalArgumentException if the actual type of the configuration parameters does not
   *     match the given type. Such mismatch might mean either a configuration error, when
   *     administrators include the wrong parameters in the start service transaction; or
   *     an error in the service itself
   * @see com.exonum.binding.common.serialization.StandardSerializers#protobuf(Class)
   */
  <MessageT extends Message> MessageT getAsMessage(Class<MessageT> parametersType);

}

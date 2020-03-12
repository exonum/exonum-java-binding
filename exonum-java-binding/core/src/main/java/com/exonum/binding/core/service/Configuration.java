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

import com.exonum.binding.common.messages.Service.ServiceConfiguration;
import com.exonum.binding.common.messages.Service.ServiceConfiguration.Format;
import com.exonum.binding.core.transaction.ExecutionContext;
import com.google.protobuf.MessageLite;
import java.util.Properties;

/**
 * Configuration parameters of Exonum service.
 *
 * <p>Network administrators agree on and pass
 * the configuration parameters as a service-specific protobuf message when adding
 * that service instance to the network. After Exonum starts the service, it
 * {@linkplain Service#initialize(ExecutionContext,
 * Configuration) passes the configuration parameters} to the newly created service instance.
 *
 * <p>Services that have few arguments are encouraged to use the standard protobuf
 * message {@link ServiceConfiguration}. It supports common text-based configuration formats.
 *
 * <p>Reconfiguration of a started service may be implemented with a supervisor service
 * and {@link Configurable} interface.
 *
 * @see Service#initialize(ExecutionContext, Configuration)
 * @see Configurable
 */
public interface Configuration {

  /**
   * Returns the configuration parameters as a Protocol Buffers message.
   *
   * <p>This method is created for flexibility and if {@link ServiceConfiguration} is used it is
   * more convenient to some another method corresponding to the configuration format.
   *
   * @param parametersType the type of a Protocol Buffers message in which the service configuration
   *     parameters are recorded in transactions starting the service instance
   * @throws IllegalArgumentException if the actual type of the configuration parameters does not
   *     match the given type. Such mismatch might mean either a configuration error, when
   *     administrators pass the wrong parameters; or an error in the service itself
   * @see com.exonum.binding.common.serialization.StandardSerializers#protobuf(Class)
   */
  <MessageT extends MessageLite> MessageT getAsMessage(Class<MessageT> parametersType);

  /**
   * Returns the configuration format.
   *
   * @throws IllegalArgumentException if the actual type of the configuration is not an instance of
   *     {@link ServiceConfiguration}
   */
  Format getConfigurationFormat();

  /**
   * Returns the configuration as a plain text string.
   *
   * @throws IllegalArgumentException if the actual type of the configuration is not an instance of
   *     {@link ServiceConfiguration}
   */
  String getAsString();

  /**
   * Returns the configuration as an object  of the given type decoded from the underlying JSON.
   *
   * @param <T> the type of the configuration object
   * @param configType the class of T
   * @throws IllegalArgumentException if the actual type of the configuration is not an instance of
   *     {@link ServiceConfiguration};
   *     or the configuration is not in the {@linkplain Format#JSON JSON} format
   * @throws com.google.gson.JsonParseException in case of JSON parse error
   */
  <T> T getAsJson(Class<T> configType);

  /**
   * Returns the configuration as a properties.
   *
   * @throws IllegalArgumentException if the actual type of the configuration is not an instance of
   *     {@link ServiceConfiguration};
   *     or the configuration is not in the {@linkplain Format#PROPERTIES properties} format;
   *     or an error occurs during parsing properties (i.e. malformed properties)
   */
  Properties getAsProperties();
}

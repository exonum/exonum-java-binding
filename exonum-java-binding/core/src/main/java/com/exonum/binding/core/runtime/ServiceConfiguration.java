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

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.common.messages.Service;
import com.exonum.binding.common.messages.Service.ServiceConfiguration.Format;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.service.Configuration;
import com.google.gson.Gson;
import com.google.protobuf.MessageLite;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Properties;

final class ServiceConfiguration implements Configuration {

  private final byte[] configuration;

  ServiceConfiguration(byte[] configuration) {
    this.configuration = configuration.clone();
  }

  @Override
  public <MessageT extends MessageLite> MessageT getAsMessage(Class<MessageT> parametersType) {
    Serializer<MessageT> serializer = StandardSerializers.protobuf(parametersType);
    return serializer.fromBytes(configuration);
  }

  @Override
  public String getAsString() {
    return validateAndGet(Format.TEXT);
  }

  @Override
  public String getAsJson() {
    return validateAndGet(Format.JSON);
  }

  @Override
  public <T> T getAsJson(Class<T> configType) {
    String configuration = getAsJson();
    return new Gson().fromJson(configuration, configType);
  }

  @Override
  public Properties getAsProperties() {
    String configuration = validateAndGet(Format.PROPERTIES);
    Properties properties = new Properties();
    try {
      properties.load(new StringReader(configuration));
    } catch (IOException e) {
      throw new IllegalArgumentException("Error reading properties configuration", e);
    }
    return properties;
  }

  @Override
  public String toString() {
    return "ServiceConfiguration{" + Arrays.toString(configuration) + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ServiceConfiguration)) {
      return false;
    }
    ServiceConfiguration that = (ServiceConfiguration) o;
    return Arrays.equals(configuration, that.configuration);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(configuration);
  }

  private String validateAndGet(Format format) {
    Service.ServiceConfiguration configuration = getAsMessage(Service.ServiceConfiguration.class);
    checkArgument(configuration.getFormat() == format,
        "Expected configuration in %s format, but actual was %s",
        format, configuration.getFormat());

    return configuration.getValue();
  }
}

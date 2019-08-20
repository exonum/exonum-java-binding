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
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.core.service.Configuration;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import java.util.Objects;

final class ServiceConfiguration implements Configuration {

  private final Any configuration;

  ServiceConfiguration(Any configuration) {
    this.configuration = checkNotNull(configuration);
  }

  @Override
  public <MessageT extends Message> MessageT getAsMessage(Class<MessageT> parametersType) {
    checkArgument(configuration.is(parametersType), "Actual type of the configuration message "
            + "specified by the network administrators (%s) does not match requested (%s)",
        configuration.getTypeUrl(), parametersType.getTypeName());

    try {
      return configuration.unpack(parametersType);
    } catch (InvalidProtocolBufferException e) {
      String message = String.format("Cannot unpack Any into %s message",
          parametersType.getTypeName());
      throw new IllegalArgumentException(message, e);
    }
  }

  @Override
  public String toString() {
    return "ServiceConfiguration{" + configuration + '}';
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
    return Objects.equals(configuration, that.configuration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(configuration);
  }
}

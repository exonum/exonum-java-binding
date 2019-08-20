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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.core.storage.indices.TestProtoMessages.Id;
import com.exonum.binding.core.storage.indices.TestProtoMessages.Point;
import com.google.protobuf.Any;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class ServiceConfigurationTest {

  @Test
  void getAsMessage() {
    // Pack the config in Any as core does
    Id config = id("1");
    Any configInTx = Any.pack(config);

    ServiceConfiguration serviceConfiguration = new ServiceConfiguration(configInTx);

    // Decode the config
    Id unpackedConfig = serviceConfiguration.getAsMessage(Id.class);

    assertThat(unpackedConfig).isEqualTo(config);
  }

  @Test
  void getAsMessageWrongType() {
    // Pack the config in Any as core does
    Id config = id("1");
    Any configInTx = Any.pack(config);

    ServiceConfiguration serviceConfiguration = new ServiceConfiguration(configInTx);

    // Try to decode the config
    Exception e = assertThrows(IllegalArgumentException.class,
        () -> serviceConfiguration.getAsMessage(Point.class));

    assertThat(e).hasMessageContainingAll(Id.getDescriptor().getName(),
        Point.getDescriptor().getName());
  }

  @Test
  void verifyEquals() {
    EqualsVerifier.forClass(ServiceConfiguration.class)
        .withPrefabValues(Any.class, Any.pack(id("Red")), Any.pack(id("Black")))
        .verify();
  }

  private static Id id(String id) {
    return Id.newBuilder()
        .setId(id)
        .build();
  }
}
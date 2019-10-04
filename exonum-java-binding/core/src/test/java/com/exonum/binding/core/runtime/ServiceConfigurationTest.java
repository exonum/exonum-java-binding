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

import static com.exonum.binding.test.Bytes.bytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.core.storage.indices.TestProtoMessages.Id;
import com.google.protobuf.InvalidProtocolBufferException;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class ServiceConfigurationTest {

  @Test
  void getAsMessage() {
    Id config = anyId();
    byte[] serializedConfig = config.toByteArray();

    ServiceConfiguration serviceConfiguration = new ServiceConfiguration(serializedConfig);

    // Decode the config
    Id unpackedConfig = serviceConfiguration.getAsMessage(Id.class);

    assertThat(unpackedConfig).isEqualTo(config);
  }

  @Test
  void getAsMessageNotMessage() {
    // Not a valid serialized Id
    byte[] serializedConfig = bytes(1, 2, 3, 4);

    ServiceConfiguration serviceConfiguration = new ServiceConfiguration(serializedConfig);

    // Try to decode the config
    Exception e = assertThrows(IllegalArgumentException.class,
        () -> serviceConfiguration.getAsMessage(Id.class));

    assertThat(e).hasCauseInstanceOf(InvalidProtocolBufferException.class);
  }

  @Test
  void verifyEquals() {
    EqualsVerifier.forClass(ServiceConfiguration.class)
        .verify();
  }

  private static Id anyId() {
    return Id.newBuilder()
        .setId("12ab")
        .build();
  }
}

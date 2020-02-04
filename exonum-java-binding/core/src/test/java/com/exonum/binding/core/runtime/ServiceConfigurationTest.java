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
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.messages.Service;
import com.exonum.binding.common.messages.Service.ServiceConfiguration.Format;
import com.exonum.binding.core.storage.indices.TestProtoMessages.Id;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.Properties;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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

  @ParameterizedTest
  @MethodSource("configurations")
  void getConfigurationFormat(Service.ServiceConfiguration configuration) {
    ServiceConfiguration serviceConfiguration =
        new ServiceConfiguration(configuration.toByteArray());

    Format actualFormat = serviceConfiguration.getConfigurationFormat();

    assertThat(actualFormat).isEqualTo(configuration.getFormat());
  }

  @Test
  void getConfigurationFormatBadFormat() {
    Service.ServiceConfiguration configuration = fooConfiguration(Format.NONE);
    ServiceConfiguration serviceConfiguration =
        new ServiceConfiguration(configuration.toByteArray());

    assertThrows(IllegalArgumentException.class, serviceConfiguration::getConfigurationFormat);
  }

  @ParameterizedTest
  @MethodSource("configurations")
  void getAsPlainString(Service.ServiceConfiguration configuration) {
    ServiceConfiguration serviceConfiguration =
        new ServiceConfiguration(configuration.toByteArray());

    String actualConfig = serviceConfiguration.getAsString();

    assertThat(actualConfig).isEqualTo(configuration.getValue());
  }

  @Test
  void getConfigurationAsStringBadFormat() {
    Service.ServiceConfiguration configuration = fooConfiguration(Format.NONE);
    ServiceConfiguration serviceConfiguration =
        new ServiceConfiguration(configuration.toByteArray());

    assertThrows(IllegalArgumentException.class, serviceConfiguration::getAsString);
  }

  private static List<Service.ServiceConfiguration> configurations() {
    return ImmutableList.of(
        fooConfiguration(Format.TEXT),
        fooConfiguration(Format.JSON),
        fooConfiguration(Format.PROPERTIES)
    );
  }

  private static Service.ServiceConfiguration fooConfiguration(Format format) {
    return Service.ServiceConfiguration.newBuilder()
        .setFormat(format)
        .setValue("foo")
        .build();
  }

  @Test
  void getAsPlainStringIsNotServiceConfiguration() {
    byte[] serializedConfig = anyId().toByteArray();
    ServiceConfiguration serviceConfiguration = new ServiceConfiguration(serializedConfig);

    assertThrows(IllegalArgumentException.class, serviceConfiguration::getAsString);
  }

  @Test
  void getAsJson() {
    byte[] configuration = Service.ServiceConfiguration.newBuilder()
        .setFormat(Format.JSON)
        .setValue("{'foo' : 'bar'}")
        .build()
        .toByteArray();
    ServiceConfiguration serviceConfiguration = new ServiceConfiguration(configuration);

    Foo actualConfig = serviceConfiguration.getAsJson(Foo.class);

    assertThat(actualConfig.foo).isEqualTo("bar");
  }

  @Test
  void getAsProperties() {
    byte[] configuration = Service.ServiceConfiguration.newBuilder()
        .setFormat(Format.PROPERTIES)
        .setValue("foo=foo\nbar=bar")
        .build()
        .toByteArray();
    ServiceConfiguration serviceConfiguration = new ServiceConfiguration(configuration);

    Properties properties = serviceConfiguration.getAsProperties();

    assertThat(properties).contains(entry("foo", "foo"), entry("bar", "bar"));
  }

  private static Id anyId() {
    return Id.newBuilder()
        .setId("12ab")
        .build();
  }

  private static class Foo {
    String foo;
  }

}

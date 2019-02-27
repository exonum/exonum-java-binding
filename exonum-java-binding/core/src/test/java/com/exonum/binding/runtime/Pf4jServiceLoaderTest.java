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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;

@ExtendWith(MockitoExtension.class)
class Pf4jServiceLoaderTest {

  @Mock
  private PluginManager pluginManager;

  @InjectMocks
  private Pf4jServiceLoader serviceLoader;

  @Test
  void canLoadService() throws ServiceLoadingException {
    URI artifactLocation = URI.create("file:///tmp/service.jar");
    String pluginId = "com.acme:foo-service:1.0.1";

    when(pluginManager.loadPlugin(eq(Paths.get(artifactLocation))))
        .thenReturn(pluginId);
    when(pluginManager.startPlugin(eq(pluginId)))
        .thenReturn(PluginState.STARTED);

    // Try to load the service
    LoadedServiceDefinition serviceDefinition = serviceLoader.loadService(artifactLocation);

    // Verify the plugin is started
    verify(pluginManager).startPlugin(eq(pluginId));

    // Check the definition
    ServiceId serviceId = serviceDefinition.getId();
    ServiceId expectedId = ServiceId.parseFrom(pluginId);
    assertThat(serviceId).isEqualTo(expectedId);
    // todo: check instantiation if we can?

    // Check the definition is accessible
    assertThat(serviceLoader.findService(serviceId)).hasValue(serviceDefinition);
  }

  @Test
  void cannotLoadIfPluginManagerReturnsNull() {
    URI artifactLocation = URI.create("file:///tmp/service.jar");
    String pluginId = "com.acme:foo-service:1.0.1";

    // The 2.x PF4J API returns null to signal that the plugin cannot be loaded
    when(pluginManager.loadPlugin(eq(Paths.get(artifactLocation))))
        .thenReturn(null);

    // Try to load the service
    Exception e = assertThrows(ServiceLoadingException.class,
        () -> serviceLoader.loadService(artifactLocation));
    assertThat(e).hasMessageContaining("PluginManager failed to load the plugin");

    // Check the definition is inaccessible
    ServiceId serviceId = ServiceId.parseFrom(pluginId);
    assertThat(serviceLoader.findService(serviceId)).isEmpty();
  }

  @Test
  void cannotLoadIfPluginFailedToStart() {
    URI artifactLocation = URI.create("file:///tmp/service.jar");
    String pluginId = "com.acme:foo-service:1.0.1";

    when(pluginManager.loadPlugin(eq(Paths.get(artifactLocation))))
        .thenReturn(pluginId);
    // In the 2.x PF4J API a failed plugin start is communicated through a plugin state
    // that is not "STARTED"
    when(pluginManager.startPlugin(eq(pluginId)))
        .thenReturn(PluginState.DISABLED);

    // Try to load the service
    Exception e = assertThrows(ServiceLoadingException.class,
        () -> serviceLoader.loadService(artifactLocation));
    assertThat(e).hasMessageContaining("PluginManager failed to start the plugin");

    // Check the definition is inaccessible
    ServiceId serviceId = ServiceId.parseFrom(pluginId);
    assertThat(serviceLoader.findService(serviceId)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "",
      "foo-service",
      "com.acme:foo-service:1.0:extra-coordinate",
  })
  void cannotLoadIfInvalidPluginIdInMetadata(String invalidPluginId) {
    URI artifactLocation = URI.create("file:///tmp/service.jar");

    when(pluginManager.loadPlugin(eq(Paths.get(artifactLocation))))
        .thenReturn(invalidPluginId);

    // Try to load the service
    Exception e = assertThrows(ServiceLoadingException.class,
        () -> serviceLoader.loadService(artifactLocation));
    assertThat(e).hasMessageContaining("Invalid plugin id: " + invalidPluginId
        + ", must be in format 'groupId:artifactId:version'");
  }


  @Test
  void canLoadUnloadService() throws ServiceLoadingException {
    URI artifactLocation = URI.create("file:///tmp/service.jar");
    String pluginId = "com.acme:foo-service:1.0.1";

    when(pluginManager.loadPlugin(eq(Paths.get(artifactLocation))))
        .thenReturn(pluginId);
    when(pluginManager.startPlugin(eq(pluginId)))
        .thenReturn(PluginState.STARTED);

    // Try to load the service
    LoadedServiceDefinition serviceDefinition = serviceLoader.loadService(artifactLocation);

    // Try to unload the service
    ServiceId serviceId = serviceDefinition.getId();
    serviceLoader.unloadService(serviceId);

    // Check properly unloaded
    verify(pluginManager).unloadPlugin(pluginId);
    assertThat(serviceLoader.findService(serviceId)).isEmpty();
  }

  @Test
  void unloadService() {
  }
}

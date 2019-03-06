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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.service.AbstractServiceModule;
import com.exonum.binding.service.ServiceModule;
import com.google.common.collect.ImmutableList;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;

@SuppressWarnings("WeakerAccess")
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

    when(pluginManager.loadPlugin(Paths.get(artifactLocation)))
        .thenReturn(pluginId);
    when(pluginManager.startPlugin(pluginId))
        .thenReturn(PluginState.STARTED);
    Class<TestModule1> moduleType = TestModule1.class;
    when(pluginManager.getExtensionClasses(ServiceModule.class, pluginId))
        .thenReturn(modules(moduleType));

    // Try to load the service
    LoadedServiceDefinition serviceDefinition = serviceLoader.loadService(artifactLocation);

    // Verify the plugin is started
    verify(pluginManager).startPlugin(pluginId);

    // Check the definition
    ServiceId serviceId = serviceDefinition.getId();
    ServiceId expectedId = ServiceId.parseFrom(pluginId);
    assertThat(serviceId).isEqualTo(expectedId);
    Supplier<ServiceModule> moduleSupplier = serviceDefinition.getModuleSupplier();
    assertThat(moduleSupplier.get()).isInstanceOf(moduleType);

    // Check the definition is accessible
    assertThat(serviceLoader.findService(serviceId)).hasValue(serviceDefinition);
  }

  @Test
  @DisplayName("Cannot load a plugin if the plugin manager returns `null` "
      + "(e.g., in case of an attempt to load a duplicate plugin or other errors)")
  void cannotLoadIfPluginManagerReturnsNull() {
    URI artifactLocation = URI.create("file:///tmp/service.jar");
    String pluginId = "com.acme:foo-service:1.0.1";

    // The 2.x PF4J API returns null to signal that the plugin cannot be loaded
    when(pluginManager.loadPlugin(Paths.get(artifactLocation)))
        .thenReturn(null);

    // Try to load the service
    Exception e = assertThrows(ServiceLoadingException.class,
        () -> serviceLoader.loadService(artifactLocation));
    assertThat(e).hasMessageContaining("Failed to load the plugin from");

    // Check the definition is inaccessible
    ServiceId serviceId = ServiceId.parseFrom(pluginId);
    assertThat(serviceLoader.findService(serviceId)).isEmpty();
  }

  @Test
  void cannotLoadIfPluginFailedToStart() {
    URI artifactLocation = URI.create("file:///tmp/service.jar");
    String pluginId = "com.acme:foo-service:1.0.1";

    when(pluginManager.loadPlugin(Paths.get(artifactLocation)))
        .thenReturn(pluginId);
    // In the 2.x PF4J API a failed plugin start is communicated through a plugin state
    // that is not "STARTED"
    when(pluginManager.startPlugin(pluginId))
        .thenReturn(PluginState.DISABLED);

    // Try to load the service
    Exception e = assertThrows(ServiceLoadingException.class,
        () -> serviceLoader.loadService(artifactLocation));
    assertThat(e).hasMessageContaining("Failed to start the plugin");

    // Check the definition is inaccessible
    ServiceId serviceId = ServiceId.parseFrom(pluginId);
    assertThat(serviceLoader.findService(serviceId)).isEmpty();

    // Check it is unloaded if failed to start
    verifyUnloaded(pluginId);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "",
      "foo-service",
      "com.acme:foo-service:1.0:extra-coordinate",
  })
  void cannotLoadIfInvalidPluginIdInMetadata(String invalidPluginId) {
    URI artifactLocation = URI.create("file:///tmp/service.jar");
    when(pluginManager.loadPlugin(Paths.get(artifactLocation)))
        .thenReturn(invalidPluginId);
    when(pluginManager.startPlugin(invalidPluginId))
        .thenReturn(PluginState.STARTED);

    // Try to load the service
    Exception e = assertThrows(ServiceLoadingException.class,
        () -> serviceLoader.loadService(artifactLocation));
    assertThat(e).hasMessageContaining("Invalid plugin id");
    assertThat(e).hasMessageContaining(invalidPluginId);

    // Check it is unloaded if failed to start
    verify(pluginManager).unloadPlugin(invalidPluginId);
  }

  @ParameterizedTest
  @MethodSource("invalidServiceModuleExtensions")
  void cannotLoadIfInvalidServiceModuleExtensions(List<Class<ServiceModule>> extensions,
      String expectedErrorPattern) {
    URI artifactLocation = URI.create("file:///tmp/service.jar");
    String pluginId = "com.acme:foo-service:1.0.1";

    when(pluginManager.loadPlugin(Paths.get(artifactLocation)))
        .thenReturn(pluginId);
    when(pluginManager.startPlugin(pluginId))
        .thenReturn(PluginState.STARTED);
    when(pluginManager.getExtensionClasses(ServiceModule.class, pluginId))
        .thenReturn(extensions);

    // Try to load the service
    Exception e = assertThrows(ServiceLoadingException.class,
        () -> serviceLoader.loadService(artifactLocation));
    assertThat(e).hasMessageContaining(pluginId);
    assertThat(e).hasMessageFindingMatch(expectedErrorPattern);

    // Check it is unloaded if failed to start
    verifyUnloaded(pluginId);
  }

  private static Collection<Arguments> invalidServiceModuleExtensions() {
    return ImmutableList.of(
        arguments(emptyList(), "must provide exactly one service module as an extension"),
        arguments(modules(TestModule1.class, TestModule2.class),
            "must provide exactly one service module as an extension"),
        arguments(modules(BadModuleInaccessibleCtor.class),
            "Cannot load a plugin.+module.+not valid")
    );
  }

  @Test
  void canLoadUnloadService() throws ServiceLoadingException {
    URI artifactLocation = URI.create("file:///tmp/service.jar");
    String pluginId = "com.acme:foo-service:1.0.1";

    when(pluginManager.loadPlugin(Paths.get(artifactLocation)))
        .thenReturn(pluginId);
    when(pluginManager.startPlugin(pluginId))
        .thenReturn(PluginState.STARTED);
    when(pluginManager.getExtensionClasses(ServiceModule.class, pluginId))
        .thenReturn(modules(TestModule1.class));
    when(pluginManager.unloadPlugin(pluginId))
        .thenReturn(true);

    // Try to load the service
    LoadedServiceDefinition serviceDefinition = serviceLoader.loadService(artifactLocation);

    // Try to unload the service
    ServiceId serviceId = serviceDefinition.getId();
    serviceLoader.unloadService(serviceId);

    // Check properly unloaded
    verifyUnloaded(pluginId);
  }

  @Test
  void unloadServiceNonLoaded() {
    ServiceId unknownPluginId = ServiceId.parseFrom("com.acme:foo-service:1.0.1");
    assertThrows(IllegalArgumentException.class,
        () -> serviceLoader.unloadService(unknownPluginId));
  }

  @Test
  void findServiceNonLoaded() {
    ServiceId unknownPluginId = ServiceId.parseFrom("com.acme:foo-service:1.0.1");
    Optional<?> serviceDefinition = serviceLoader.findService(unknownPluginId);
    assertThat(serviceDefinition).isEmpty();
  }

  /**
   * Converts a variable argument parameter of {@code Class<? extends T>} to
   * a {@code List<Class<T>>}. It is required because that is what
   * {@link PluginManager#getExtensionClasses(Class)} returns.
   *
   * <p>A simple
   * <pre>
   *    when(pluginManager.getExtensionClasses(ServiceModule.class, pluginId))
   *        .thenReturn(ImmutableList.of(TestModule1.class));
   *</pre>
   * does not currently work.
   *
   * <p>See issue TODO:
   */
  @SuppressWarnings("unchecked")
  @SafeVarargs
  private static <T> List<Class<T>> modules(Class<? extends T>... modules) {
    return Arrays.stream(modules)
        .map(moduleClass -> (Class<T>) moduleClass)
        .collect(toList());
  }

  private void verifyUnloaded(String pluginId) {
    verify(pluginManager).unloadPlugin(pluginId);

    ServiceId serviceId = ServiceId.parseFrom(pluginId);
    assertThat(serviceLoader.findService(serviceId)).isEmpty();
  }

  static final class TestModule1 extends AbstractServiceModule {}

  static final class TestModule2 extends AbstractServiceModule {}

  static final class BadModuleInaccessibleCtor extends AbstractServiceModule {
    private BadModuleInaccessibleCtor() {}
  }
}

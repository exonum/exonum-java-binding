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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.exonum.binding.service.Service;
import com.exonum.binding.service.ServiceModule;
import com.exonum.binding.test.runtime.ServiceArtifactBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.inject.Guice;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.pf4j.Plugin;
import org.pf4j.PluginManager;

abstract class Pf4jServiceLoaderIntegrationTestable {

  private static final String PLUGIN_ID = "com.acme:foo-service:1.0.1";
  private static final Map<String, Class<?>> TEST_DEPENDENCY_REFERENCE_CLASSES = ImmutableMap.of(
      "exonum-java-binding", Service.class,
      "vertx", Vertx.class,
      "guice", Guice.class,
      "pf4j", PluginManager.class,
      "gson", Gson.class
  );

  private PluginManager pluginManager;
  private Pf4jServiceLoader serviceLoader;
  private Path artifactLocation;

  @BeforeEach
  void setUp(@TempDir Path tmp) {
    pluginManager = spy(createPluginManager());
    serviceLoader = new Pf4jServiceLoader(pluginManager,
        new ClassLoadingScopeChecker(TEST_DEPENDENCY_REFERENCE_CLASSES));
    artifactLocation = tmp.resolve("service.jar");
  }

  abstract PluginManager createPluginManager();

  @Test
  void canLoadService() throws ServiceLoadingException, IOException {
    String pluginId = PLUGIN_ID;
    Class<?> moduleType = TestServiceModule1.class;

    anArtifact()
        .setPluginId(pluginId)
        .addExtensionClass(moduleType)
        .writeTo(artifactLocation);

    // Try to load the service
    LoadedServiceDefinition serviceDefinition = serviceLoader.loadService(artifactLocation);

    // Check the definition
    ServiceId serviceId = serviceDefinition.getId();
    ServiceId expectedId = ServiceId.parseFrom(pluginId);
    assertThat(serviceId).isEqualTo(expectedId);
    Supplier<ServiceModule> moduleSupplier = serviceDefinition.getModuleSupplier();
    ServiceModule module = moduleSupplier.get();
    assertNotNull(module);
    // We can't check for class equality because the classloaders are different.
    assertNamesEqual(module.getClass(), moduleType);

    // Check the definition is accessible
    assertThat(serviceLoader.findService(serviceId)).hasValue(serviceDefinition);
  }

  @Test
  @DisplayName("Cannot load a plugin if the plugin manager returns `null` "
      + "(e.g., in case of an attempt to load a duplicate plugin or other errors)")
  void cannotLoadIfPluginManagerFailsToLoad() throws IOException {
    anArtifact()
        .setPluginId(PLUGIN_ID)
        // Set invalid version to fail the loading
        .setPluginVersion("")
        .writeTo(artifactLocation);

    // Try to load the service
    Exception e = assertThrows(ServiceLoadingException.class,
        () -> serviceLoader.loadService(artifactLocation));
    assertThat(e).hasMessageContaining("Failed to load the service from");

    // Check the definition is inaccessible
    ServiceId serviceId = ServiceId.parseFrom(PLUGIN_ID);
    assertThat(serviceLoader.findService(serviceId)).isEmpty();
  }

  @Test
  void cannotLoadIfNoArtifact() {
    // Try to load the service
    Exception e = assertThrows(ServiceLoadingException.class,
        () -> serviceLoader.loadService(artifactLocation));
    assertThat(e).hasMessageContaining("Failed to load");
    assertThat(e).hasMessageContaining(artifactLocation.toString());
  }


  @Test
  void cannotLoadIfPluginFailedToStart() throws IOException {
    String pluginId = PLUGIN_ID;
    Class<? extends Plugin> evilPlugin = EvilPluginFailingToStart.class;
    anArtifact()
        .setPluginId(pluginId)
        .addClass(evilPlugin)
        .setManifestEntry("Plugin-Class", evilPlugin.getName())
        .writeTo(artifactLocation);

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
      "foo-service",
      "com.acme:foo-service:1.0:extra-coordinate",
  })
  void cannotLoadIfInvalidPluginIdInMetadata(String invalidPluginId) throws IOException {
    anArtifact()
        .setPluginId(invalidPluginId)
        .writeTo(artifactLocation);

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
  void cannotLoadIfInvalidServiceModuleExtensions(
      List<Class<? extends ServiceModule>> extensionClasses, String expectedErrorPattern)
      throws IOException {
    String pluginId = PLUGIN_ID;

    anArtifact()
        .setPluginId(pluginId)
        .setExtensionClasses(extensionClasses)
        .writeTo(artifactLocation);

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
        arguments(asList(TestServiceModule1.class, TestServiceModule2.class),
            "must provide exactly one service module as an extension.+2 modules found:"),
        arguments(singletonList(TestServiceModuleInaccessibleCtor.class),
            "Cannot load a plugin.+module.+not valid")
    );
  }

  @Test
  void cannotLoadIfArtifactIncludesCopiesOfAppClasses() throws IOException {
    String pluginId = PLUGIN_ID;

    anArtifact()
        .setPluginId(pluginId)
        .addClasses(TEST_DEPENDENCY_REFERENCE_CLASSES.values())
        .writeTo(artifactLocation);

    Exception e = assertThrows(ServiceLoadingException.class,
        () -> serviceLoader.loadService(artifactLocation));
    Throwable cause = e.getCause();
    for (String dependencyName : TEST_DEPENDENCY_REFERENCE_CLASSES.keySet()) {
      assertThat(cause).hasMessageContaining(dependencyName);
    }

    // Check it is unloaded if failed to start
    verifyUnloaded(pluginId);
  }

  @Test
  void canLoadUnloadService() throws Exception {
    String pluginId = PLUGIN_ID;

    anArtifact()
        .setPluginId(pluginId)
        .writeTo(artifactLocation);

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
    ServiceId unknownPluginId = ServiceId.parseFrom(PLUGIN_ID);
    assertThrows(IllegalArgumentException.class,
        () -> serviceLoader.unloadService(unknownPluginId));
  }

  @Test
  void findServiceNonLoaded() {
    ServiceId unknownPluginId = ServiceId.parseFrom(PLUGIN_ID);
    Optional<?> serviceDefinition = serviceLoader.findService(unknownPluginId);
    assertThat(serviceDefinition).isEmpty();
  }

  /**
   * Creates a builder producing a valid artifact with some default values.
   */
  private static ServiceArtifactBuilder anArtifact() {
    Class<?> serviceModule = TestServiceModule1.class;
    return new ServiceArtifactBuilder()
        .setPluginId(PLUGIN_ID)
        .setPluginVersion("1.0.3")
        .addExtensionClass(serviceModule);
  }

  private void verifyUnloaded(String pluginId) {
    verify(pluginManager).unloadPlugin(pluginId);

    ServiceId serviceId = ServiceId.parseFrom(pluginId);
    assertThat(serviceLoader.findService(serviceId)).isEmpty();
  }

  private static void assertNamesEqual(Class<?> actual, Class<?> expected) {
    assertThat(actual.getName()).isEqualTo(expected.getName());
  }
}

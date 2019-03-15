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

package com.exonum.binding.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.test.runtime.testplugin.TestPlugin;
import com.exonum.binding.test.runtime.testplugin.TestServiceExtension;
import com.exonum.binding.test.runtime.testplugin.TestServiceExtensionImpl;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;

class ServiceArtifactBuilderSmokeIntegrationTest {

  @Test
  @DisplayName("Created plugin must be successfully loaded and unloaded by the PluginManager. "
      + "If this test does not work, subsequent use of ServiceArtifactBuilder in other ITs makes "
      + "no sense.")
  void createdArtifactCanBeLoaded(@TempDir Path tmp) throws IOException {
    Path pluginPath = tmp.resolve("test-plugin.jar");

    String pluginId = "test-plugin";
    String version = "1.0.1";
    Class<?> pluginClass = TestPlugin.class;
    new ServiceArtifactBuilder()
        .setPluginId(pluginId)
        .setPluginVersion(version)
        .setManifestEntry("Plugin-Class", pluginClass.getName())
        .addClass(pluginClass)
        .addExtensionClass(TestServiceExtensionImpl.class)
        .writeTo(pluginPath);

    PluginManager pluginManager = new DefaultPluginManager();

    // Try to load the plugin
    String loadedPluginId = pluginManager.loadPlugin(pluginPath);
    assertThat(loadedPluginId).isEqualTo(pluginId);

    // Check it has correct version
    PluginWrapper plugin = pluginManager.getPlugin(pluginId);
    assertThat(plugin.getDescriptor().getVersion()).isEqualTo(version);
    assertNamesEqual(plugin.getPlugin().getClass(), pluginClass);

    // Try to start
    PluginState pluginState = pluginManager.startPlugin(pluginId);
    assertThat(pluginState).isEqualTo(PluginState.STARTED);

    // Check the extensions
    List<Class<TestServiceExtension>> extensionClasses = pluginManager
        .getExtensionClasses(TestServiceExtension.class, pluginId);
    assertThat(extensionClasses).hasSize(1);
    Class<?> extensionType = extensionClasses.get(0);
    assertNamesEqual(extensionType, TestServiceExtensionImpl.class);


    // Try to stop and unload
    pluginState = pluginManager.stopPlugin(pluginId);
    assertThat(pluginState).isEqualTo(PluginState.STOPPED);
    boolean unloadResult = pluginManager.unloadPlugin(pluginId);
    assertTrue(unloadResult);
  }

  private static void assertNamesEqual(Class<?> actual, Class<?> expected) {
    assertThat(actual.getName()).isEqualTo(expected.getName());
  }
}

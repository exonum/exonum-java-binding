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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.service.ServiceModule;
import com.exonum.binding.test.runtime.ServiceArtifactBuilder;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;

/**
 * Tests that our configuration works in a basic scenario of loading a JAR service.
 */
class JarPluginManagerSmokeIntegrationTest {

  @Test
  void loadsUnloadsJarPlugins(@TempDir Path tmp) throws Exception {
    Path pluginPath = tmp.resolve("test-plugin.jar");

    String pluginId = "test-plugin";
    String version = "1.0.1";
    new ServiceArtifactBuilder()
        .setPluginId(pluginId)
        .setPluginVersion(version)
        .addExtensionClass(TestServiceModule1.class)
        .writeTo(pluginPath);

    PluginManager pluginManager = new JarPluginManager();

    // Try to load the plugin
    String loadedPluginId = pluginManager.loadPlugin(pluginPath);
    assertThat(loadedPluginId).isEqualTo(pluginId);

    // Try to start
    PluginState pluginState = pluginManager.startPlugin(pluginId);
    assertThat(pluginState).isEqualTo(PluginState.STARTED);

    // Check the extensions
    List<Class<? extends ServiceModule>> extensionClasses = pluginManager
        .getExtensionClasses(ServiceModule.class, pluginId);
    assertThat(extensionClasses).hasSize(1);
    Class<?> extensionType = extensionClasses.get(0);
    assertNamesEqual(extensionType, TestServiceModule1.class);

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

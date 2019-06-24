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

import static java.util.Collections.emptyList;

import java.nio.file.Path;
import java.util.List;
import org.pf4j.DefaultPluginManager;
import org.pf4j.JarPluginLoader;
import org.pf4j.ManifestPluginDescriptorFinder;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.PluginLoader;
import org.pf4j.PluginRepository;

/**
 * A plugin manager that strips most operations that use ZIP plugin format, because we only
 * support JARs. That allows to reduce the number of I/O operations during plugin loading
 * and the number of redundant log messages.
 *
 * <p><b>If anything, this plugin manager can be utilized and replaced with the
 * {@link DefaultPluginManager}.</b>
 */
class JarPluginManager extends DefaultPluginManager {

  @Override
  protected PluginDescriptorFinder createPluginDescriptorFinder() {
    // JARs use manifest only
    return new ManifestPluginDescriptorFinder();
  }

  @Override
  protected PluginRepository createPluginRepository() {
    // We do not use an abstraction of a repository, because plugins (= services) are loaded
    // explicitly. Dynamic services *might* introduce some kind of a repository where it
    // might make sense to reconsider using this abstraction.
    return new PluginRepository() {
      @Override
      public List<Path> getPluginPaths() {
        return emptyList();
      }

      @Override
      public boolean deletePluginPath(Path pluginPath) {
        return false;
      }
    };
  }

  @Override
  protected PluginLoader createPluginLoader() {
    // Returns a JarPluginLoader as it is the only format we support
    return new JarPluginLoader(this);
  }
}

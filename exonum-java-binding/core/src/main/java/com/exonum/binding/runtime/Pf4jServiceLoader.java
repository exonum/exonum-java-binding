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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import java.net.URI;
import java.util.Optional;
import org.pf4j.PluginManager;

/**
 * A loader of services as PF4J plugins. Such plugins are required to have PluginId set in
 * a certain format ('groupId:artifactId:version'); have TODO:
 *
 * @see <a href="https://pf4j.org/doc/getting-started.html">PF4J docs</a>
 */
class Pf4jServiceLoader implements ServiceLoader {

  private final PluginManager pluginManager;

  @Inject
  Pf4jServiceLoader(PluginManager pluginManager) {
    this.pluginManager = checkNotNull(pluginManager);
  }

  /**
   * Loads a service as PF4J artifact.
   *
   * <p>Verification steps involve metadata validation, starting the plugin and
   * TODO: do we instantiate any extensions? Or access extra metadata?
   */
  @Override
  public /* todo: specialize? */ LoadedServiceDefinition loadService(URI artifactLocation) throws ServiceLoadingException {
    return null;
  }

  @Override
  public Optional<LoadedServiceDefinition> findService(ServiceId serviceId) {
    return Optional.empty();
  }

  @Override
  public void unloadService(ServiceId serviceId) {

  }
}

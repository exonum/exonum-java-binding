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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;

/**
 * A loader of services as PF4J plugins. Such plugins are required to have PluginId set in
 * a certain format ('groupId:artifactId:version'); have TODO:
 *
 * @see <a href="https://pf4j.org/doc/getting-started.html">PF4J docs</a>
 */
final class Pf4jServiceLoader implements ServiceLoader {

  private static final Comparator<ServiceId> SERVICE_ID_COMPARATOR =
      Comparator.comparing(ServiceId::getGroupId)
          .thenComparing(ServiceId::getArtifactId)
          .thenComparing(ServiceId::getVersion);

  private final PluginManager pluginManager;
  private final SortedMap<ServiceId, LoadedServiceDefinition> loadedServices;

  @Inject
  Pf4jServiceLoader(PluginManager pluginManager) {
    this.pluginManager = checkNotNull(pluginManager);
    loadedServices = new TreeMap<>(SERVICE_ID_COMPARATOR);
  }

  /**
   * Loads a service as PF4J artifact.
   *
   * <p>Verification steps involve metadata validation, starting the plugin and
   * TODO: do we instantiate any extensions? Or access extra metadata?
   */
  @Override
  public /* todo: specialize? */ LoadedServiceDefinition loadService(URI artifactLocation)
      throws ServiceLoadingException {
    Path artifactPath = Paths.get(artifactLocation);
    // todo: prevent loading of duplicates at this point!
    // fixme: The plugin manager might load duplicate plugins if they have different paths!
    //   Submit an issue to fix that — it will silently load two.
    String pluginId = pluginManager.loadPlugin(artifactPath);
    if (pluginId == null) {
      throw new ServiceLoadingException("Failed to load the plugin at "
          + artifactLocation);
    }

    try {
      PluginState pluginState = pluginManager.startPlugin(pluginId);
      if (pluginState != PluginState.STARTED) {
        throw new ServiceLoadingException(
            String.format("Failed to start the plugin %s, state=%s", pluginId, pluginState));
      }

      ServiceId serviceId = ServiceId.parseFrom(pluginId);
      LoadedServiceDefinition serviceDefinition =
          DefaultLoadedServiceDefinition.newInstance(serviceId);

      loadedServices.put(serviceId, serviceDefinition);

      return serviceDefinition;
    } catch (IllegalArgumentException e) {
      pluginManager.unloadPlugin(pluginId);
      throw new ServiceLoadingException(e);
    } catch (Exception e) {
      pluginManager.unloadPlugin(pluginId);
      throw e;
    }
  }

  @Override
  public Optional<LoadedServiceDefinition> findService(ServiceId serviceId) {
    return Optional.ofNullable(loadedServices.get(serviceId));
  }

  @Override
  public void unloadService(ServiceId serviceId) {
    checkArgument(loadedServices.containsKey(serviceId), "No such serviceId: %s", serviceId);

    String pluginId = serviceId.toString();
    // Fixme: the docs don't say why it may fail to stop the plugin.
    //   Fire an issue?
    boolean stopped = pluginManager.unloadPlugin(pluginId);
    checkState(stopped, "Unknown error whilst unloading the plugin");

    loadedServices.remove(serviceId);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("loadedServices", loadedServices)
        .toString();
  }
}

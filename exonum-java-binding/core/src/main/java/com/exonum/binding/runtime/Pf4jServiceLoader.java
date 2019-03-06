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

import com.exonum.binding.service.ServiceModule;
import com.google.common.base.MoreObjects;
import com.google.inject.Inject;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;

/**
 * A loader of services as PF4J plugins. Such plugins are required to have PluginId set in
 * a certain format ('groupId:artifactId:version'); have a single {@link ServiceModule} as
 * an extension.
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
   * <p>Verification steps involve metadata validation, starting the plugin and checking it provides
   * a single ServiceModule as an extension.
   */
  @Override
  public LoadedServiceDefinition loadService(URI artifactLocation)
      throws ServiceLoadingException {
    // Load a plugin
    String pluginId = loadPlugin(artifactLocation);
    try {
      // Start the plugin
      startPlugin(pluginId);

      // Load the service definition
      return loadDefinition(pluginId);
    } catch (IllegalArgumentException e) {
      pluginManager.unloadPlugin(pluginId);
      throw new ServiceLoadingException(e);
    } catch (Exception e) {
      pluginManager.unloadPlugin(pluginId);
      throw e;
    }
  }

  private String loadPlugin(URI artifactLocation) throws ServiceLoadingException {
    Path artifactPath = Paths.get(artifactLocation);
    // fixme: prevent loading of duplicates at this point. The plugin manager might load duplicate
    //  plugins if they have different paths. This problem is resolved
    //  in https://github.com/pf4j/pf4j/pull/287 , update PF4J when the fix is released.
    String pluginId = pluginManager.loadPlugin(artifactPath);
    if (pluginId == null) {
      throw new ServiceLoadingException("Failed to load the plugin from "
          + artifactLocation);
    }
    return pluginId;
  }

  private void startPlugin(String pluginId) throws ServiceLoadingException {
    PluginState pluginState = pluginManager.startPlugin(pluginId);
    if (pluginState != PluginState.STARTED) {
      throw new ServiceLoadingException(
          String.format("Failed to start the plugin %s, its state=%s", pluginId, pluginState));
    }
  }

  /** Loads the service definition from the already loaded plugin with the given id. */
  private LoadedServiceDefinition loadDefinition(String pluginId) throws ServiceLoadingException {
    ServiceId serviceId = extractServiceId(pluginId);
    Supplier<ServiceModule> serviceModuleSupplier = findServiceModuleSupplier(pluginId);
    LoadedServiceDefinition serviceDefinition =
        LoadedServiceDefinition.newInstance(serviceId, serviceModuleSupplier);

    assert !loadedServices.containsKey(serviceId);
    loadedServices.put(serviceId, serviceDefinition);
    return serviceDefinition;
  }

  private static ServiceId extractServiceId(String pluginId) throws ServiceLoadingException {
    try {
      return ServiceId.parseFrom(pluginId);
    } catch (IllegalArgumentException e) {
      throw new ServiceLoadingException(
          String.format("Invalid plugin id (%s) is specified in service artifact metadata, "
              + "must be in format 'groupId:artifactId:version'", pluginId));
    }
  }

  private Supplier<ServiceModule> findServiceModuleSupplier(String pluginId)
      throws ServiceLoadingException {
    List<Class<ServiceModule>> extensionClasses = pluginManager
        .getExtensionClasses(ServiceModule.class, pluginId);
    checkArgument(extensionClasses.size() == 1,
        "A plugin (%s) must provide exactly one service module as an extension, "
            + "but %s found: %s", pluginId, extensionClasses.size(), extensionClasses);

    Class<ServiceModule> serviceModuleClass = extensionClasses.get(0);
    try {
      return new ReflectiveModuleSupplier(serviceModuleClass);
    } catch (NoSuchMethodException | IllegalAccessException e) {
      String message = String.format("Cannot load a plugin (%s): module (%s) is not valid",
          pluginId, serviceModuleClass);
      throw new ServiceLoadingException(message, e);
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
    //   Follow: https://github.com/pf4j/pf4j/issues/291
    boolean stopped = pluginManager.unloadPlugin(pluginId);
    checkState(stopped, "Unknown error whilst unloading the plugin (%s)", pluginId);

    loadedServices.remove(serviceId);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("loadedServices", loadedServices)
        .toString();
  }
}

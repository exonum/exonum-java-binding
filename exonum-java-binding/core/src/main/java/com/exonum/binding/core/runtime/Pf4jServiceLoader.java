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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.core.service.ServiceModule;
import com.google.common.base.MoreObjects;
import com.google.inject.Inject;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;
import org.pf4j.Extension;
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
  private final ClassLoadingScopeChecker classLoadingChecker;
  private final SortedMap<ServiceId, LoadedServiceDefinition> loadedServices;

  @Inject
  Pf4jServiceLoader(PluginManager pluginManager, ClassLoadingScopeChecker classLoadingChecker) {
    this.pluginManager = checkNotNull(pluginManager);
    this.classLoadingChecker = classLoadingChecker;
    loadedServices = new TreeMap<>(SERVICE_ID_COMPARATOR);
  }

  /**
   * Loads a service as PF4J artifact.
   *
   * <p>Verification steps involve metadata validation, starting the plugin and checking it provides
   * a single ServiceModule as an extension.
   */
  @Override
  public LoadedServiceDefinition loadService(Path artifactPath)
      throws ServiceLoadingException {
    // Load a plugin
    String pluginId = loadPlugin(artifactPath);
    try {
      // Verify the plugin
      verifyPostLoad(pluginId);

      // Start the plugin
      startPlugin(pluginId);

      // Load the service definition
      return loadDefinition(pluginId);
    } catch (IllegalArgumentException e) {
      unloadPlugin(pluginId);
      throw new ServiceLoadingException(String.format("Failed to load plugin %s:", pluginId), e);
    } catch (Exception e) {
      unloadPlugin(pluginId);
      throw e;
    }
  }

  private String loadPlugin(Path artifactLocation) throws ServiceLoadingException {
    try {
      return pluginManager.loadPlugin(artifactLocation);
    } catch (Exception e) {
      throw new ServiceLoadingException("Failed to load the service from " + artifactLocation, e);
    }
  }

  private void verifyPostLoad(String pluginId) {
    // Check no copies of application classes are included in the artifact
    ClassLoader pluginClassLoader = pluginManager.getPluginClassLoader(pluginId);
    classLoadingChecker.checkNoCopiesOfAppClasses(pluginClassLoader);
  }

  private void startPlugin(String pluginId) throws ServiceLoadingException {
    try {
      PluginState pluginState = pluginManager.startPlugin(pluginId);
      checkState(pluginState == PluginState.STARTED,
          "Failed to start the plugin %s, its state=%s", pluginId, pluginState);
    } catch (Exception e) {
      // Catch any exception, as it may originate either from PluginManager code or
      // from Plugin#start (= service code).
      throw new ServiceLoadingException("Failed to start the plugin " + pluginId, e);
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
    List<Class<? extends ServiceModule>> extensionClasses = pluginManager
        .getExtensionClasses(ServiceModule.class, pluginId);
    checkServiceModules(pluginId, extensionClasses);

    Class<? extends ServiceModule> serviceModuleClass = extensionClasses.get(0);
    try {
      return new ReflectiveModuleSupplier(serviceModuleClass);
    } catch (NoSuchMethodException | IllegalAccessException e) {
      String message = String.format("Cannot load a plugin (%s): module (%s) is not valid",
          pluginId, serviceModuleClass);
      throw new ServiceLoadingException(message, e);
    }
  }

  private void checkServiceModules(String pluginId, List<Class<? extends ServiceModule>> extensions)
      throws ServiceLoadingException {
    int numServiceModules = extensions.size();
    if (numServiceModules == 1) {
      return;
    }
    String message;
    if (numServiceModules == 0) {
      message = String.format("A plugin (%s) must provide exactly one service module as "
              + "an extension, but no modules found.%nCheck that your %s implementation "
              + "is annotated with @%s",
          pluginId, ServiceModule.class.getSimpleName(), Extension.class.getSimpleName());
    } else {
      message = String.format("A plugin (%s) must provide exactly one service module as "
              + "an extension, but %d modules found:%n%s.%nMultiple modules are not currently "
              + "supported, but please let us know if you need them.",
          pluginId, numServiceModules, extensions);
    }
    throw new ServiceLoadingException(message);
  }

  private void unloadPlugin(String pluginId) {
    pluginManager.unloadPlugin(pluginId);
  }

  @Override
  public Optional<LoadedServiceDefinition> findService(ServiceId serviceId) {
    return Optional.ofNullable(loadedServices.get(serviceId));
  }

  @Override
  public void unloadService(ServiceId serviceId) {
    checkArgument(loadedServices.containsKey(serviceId), "No such serviceId: %s", serviceId);

    String pluginId = serviceId.toString();
    try {
      boolean stopped = pluginManager.unloadPlugin(pluginId);
      // The docs don't say why it may fail to stop the plugin.
      // Follow: https://github.com/pf4j/pf4j/issues/291
      checkState(stopped, "Unknown error whilst unloading the plugin (%s)", pluginId);
    } finally {
      loadedServices.remove(serviceId);
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("loadedServices", loadedServices)
        .toString();
  }
}

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

package com.exonum.binding.app;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.runtime.FrameworkModule;
import com.exonum.binding.runtime.ServiceRuntime;
import com.exonum.binding.service.Service;
import com.exonum.binding.time.TimeSchema;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.pf4j.PluginManager;

/**
 * A bootstrap loader of the service runtime.
 */
public final class ServiceRuntimeBootstrap {

  /**
   * The application stage, configuring Guice behaviour. Can be made configurable if needed.
   */
  private static final Stage APP_STAGE = Stage.PRODUCTION;

  private static final ImmutableMap<String, Class<?>> DEPENDENCY_REFERENCE_CLASSES =
      ImmutableMap.<String, Class<?>>builder()
          .put("exonum-java-binding-core", Service.class)
          .put("exonum-java-binding-common", HashCode.class)
          .put("exonum-time-oracle", TimeSchema.class)
          .put("vertx", Vertx.class)
          .put("gson", Gson.class)
          .put("guava", ImmutableMap.class)
          .put("guice", Guice.class)
          .put("pf4j", PluginManager.class)
          .put("log4j", LogManager.class)
          .build();

  /**
   * Bootstraps a Java service runtime.
   *
   * @param serverPort a port for the web server providing transport to Java services
   * @return a new service runtime
   */
  public static ServiceRuntime createServiceRuntime(int serverPort) {
    // Load the native libraries
    LibraryLoader.load();

    // Create the framework injector
    Module frameworkModule = new FrameworkModule(serverPort, DEPENDENCY_REFERENCE_CLASSES);
    Injector frameworkInjector = Guice.createInjector(APP_STAGE, frameworkModule);

    return frameworkInjector.getInstance(ServiceRuntime.class);
  }

  private ServiceRuntimeBootstrap() {}
}

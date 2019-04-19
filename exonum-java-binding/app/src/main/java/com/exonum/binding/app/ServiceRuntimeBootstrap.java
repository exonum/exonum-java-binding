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

import com.exonum.binding.runtime.FrameworkModule;
import com.exonum.binding.runtime.ServiceRuntime;
import com.exonum.binding.util.LibraryLoader;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;

/**
 * A bootstrap loader of the service runtime.
 */
public final class ServiceRuntimeBootstrap {

  /**
   * The application stage, configuring Guice behaviour. Can be made configurable if needed.
   */
  private static final Stage APP_STAGE = Stage.PRODUCTION;

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
    Module frameworkModule = new FrameworkModule(serverPort);
    Injector frameworkInjector = Guice.createInjector(APP_STAGE, frameworkModule);

    return frameworkInjector.getInstance(ServiceRuntime.class);
  }

  private ServiceRuntimeBootstrap() {}
}

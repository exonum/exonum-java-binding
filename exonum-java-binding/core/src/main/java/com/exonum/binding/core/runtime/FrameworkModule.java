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

import static com.google.inject.name.Names.named;

import com.exonum.binding.core.service.adapters.ViewFactory;
import com.exonum.binding.core.service.adapters.ViewProxyFactory;
import com.exonum.binding.core.transport.Server;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import java.util.Map;

/**
 * A framework module which configures the system-wide bindings.
 */
public final class FrameworkModule extends AbstractModule {

  static final String SERVICE_WEB_SERVER_PORT = "Service web server port";

  private final int serviceWebServerPort;
  private final ImmutableMap<String, Class<?>> dependencyReferenceClasses;

  /**
   * Creates a framework module with the given configuration.
   *
   * @param serviceWebServerPort the port for the web server on which endpoints of Exonum services
   *     will be mounted
   * @param dependencyReferenceClasses the reference classes from framework-provided dependencies
   */
  public FrameworkModule(int serviceWebServerPort,
      Map<String, Class<?>> dependencyReferenceClasses) {
    this.serviceWebServerPort = serviceWebServerPort;
    this.dependencyReferenceClasses = ImmutableMap.copyOf(dependencyReferenceClasses);
  }

  @Override
  protected void configure() {
    // Install the runtime module
    install(new RuntimeModule(dependencyReferenceClasses));

    // Specify framework-wide bindings
    bind(Server.class).toProvider(Server::create)
        .in(Singleton.class);
    bind(Integer.class).annotatedWith(named(SERVICE_WEB_SERVER_PORT))
        .toInstance(serviceWebServerPort);

    bind(ViewFactory.class).toInstance(ViewProxyFactory.getInstance());
    // todo: Consider providing an implementation of a Node —
    //   requires changing its contract.
  }
}

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

import static com.google.inject.name.Names.named;

import com.exonum.binding.service.adapters.ViewFactory;
import com.exonum.binding.service.adapters.ViewProxyFactory;
import com.exonum.binding.transport.Server;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;

/**
 * A framework module which configures the system-wide bindings.
 */
final class FrameworkModule extends AbstractModule {

  static final String SERVICE_WEB_SERVER_PORT = "Service web server port";

  private final int serviceWebServerPort;

  FrameworkModule(int serviceWebServerPort) {
    this.serviceWebServerPort = serviceWebServerPort;
  }

  @Override
  protected void configure() {
    bind(PluginManager.class).to(DefaultPluginManager.class)
        .in(Singleton.class);
    bind(ServiceLoader.class).to(Pf4jServiceLoader.class)
        .in(Singleton.class);

    bind(Server.class).toProvider(Server::create)
        .in(Singleton.class);
    bind(Integer.class).annotatedWith(named(SERVICE_WEB_SERVER_PORT))
        .toInstance(serviceWebServerPort);

    bind(ViewFactory.class).toInstance(ViewProxyFactory.getInstance());
    // todo: Consider providing an implementation of a Node —
    //   requires changing its contract.
  }
}

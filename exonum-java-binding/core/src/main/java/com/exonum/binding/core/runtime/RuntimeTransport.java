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

import static com.exonum.binding.core.runtime.FrameworkModule.SERVICE_WEB_SERVER_PORT;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.core.transport.Server;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Runtime transport connects service APIs to the web-server.
 */
public final class RuntimeTransport implements AutoCloseable {

  private static Logger logger = LogManager.getLogger(RuntimeTransport.class);

  private final Server server;
  private final int port;

  /**
   * Creates a new runtime transport.
   *
   * @param server a web server providing transport to Java services
   * @param port a port for the web server providing transport to Java services
   */
  @Inject
  public RuntimeTransport(Server server, @Named(SERVICE_WEB_SERVER_PORT) int port) {
    this.server = checkNotNull(server);
    this.port = port;
  }

  /**
   * Starts the web server.
   */
  void start() {
    try {
      server.start(port).get();
    } catch (InterruptedException | ExecutionException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Connects the API of a started service to the web-server.
   */
  void connectServiceApi(ServiceWrapper service) {
    // Create the service API handlers
    Router router = server.createRouter();
    service.createPublicApiHandlers(router);

    // Mount the service handlers
    String serviceApiPath = createServiceApiPath(service);
    server.mountSubRouter(serviceApiPath, router);

    // Log the endpoints
    logApiMountEvent(service, serviceApiPath, router);
  }

  private static String createServiceApiPath(ServiceWrapper service) {
    String servicePathFragment = service.getPublicApiRelativePath();
    return ServiceRuntime.API_ROOT_PATH + "/" + servicePathFragment;
  }

  private void logApiMountEvent(ServiceWrapper service, String serviceApiPath, Router router) {
    List<Route> serviceRoutes = router.getRoutes();
    if (serviceRoutes.isEmpty()) {
      // The service has no API: nothing to log
      return;
    }

    String serviceName = service.getName();
    int port = server.getActualPort().orElse(0);
    // Currently the API is mounted on *all* interfaces, see VertxServer#start
    logger.info("Service {} API is mounted at <host>::{}{}", serviceName, port, serviceApiPath);

    // Log the full path to one of the service endpoint
    serviceRoutes.stream()
        .map(Route::getPath)
        .filter(Objects::nonNull) // null routes are possible in failure handlers, for instance
        .findAny()
        .ifPresent(someRoute ->
            logger.info("    E.g.: http://127.0.0.1:{}{}", port, serviceApiPath + someRoute)
        );
  }

  /**
   * Stops the web-server.
   */
  @Override
  public void close() throws InterruptedException {
    try {
      server.stop().get();
    } catch (ExecutionException e) {
      throw new IllegalStateException(e.getCause());
    }
  }
}

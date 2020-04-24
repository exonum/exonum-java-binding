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

package com.exonum.binding.core.transport;

import static com.exonum.binding.core.transport.VertxServer.State.IDLE;
import static com.exonum.binding.core.transport.VertxServer.State.STARTED;
import static com.exonum.binding.core.transport.VertxServer.State.STOPPED;

import com.google.common.annotations.VisibleForTesting;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An HTTP server providing transport for Exonum transactions and read-requests.
 *
 * <p>This class hides some nuances of using vert.x as an HTTP server from other components.
 *
 * <p>The class is thread-safe. It does not support client-side locking.
 */
final class VertxServer implements Server {
  private static final Logger logger = LogManager.getLogger(VertxServer.class);

  private final Vertx vertx;
  private final HttpServer server;
  private final Router rootRouter;
  private final Object lock = new Object();

  enum State {
    IDLE,
    STARTED,
    STOPPED
  }

  private State state;
  private CompletableFuture<Void> stopFuture;

  /**
   * Creates an HTTP server with no request handlers.
   *
   * <p>Use {@link #start(int)} to start listening to incoming requests.
   */
  VertxServer() {
    vertx = Vertx.vertx();
    rootRouter = Router.router(vertx);
    server = vertx.createHttpServer()
        .requestHandler(rootRouter);
    state = IDLE;
  }

  @Override
  public Router createRouter() {
    synchronized (lock) {
      checkNotStopped();
      return Router.router(vertx);
    }
  }

  @Override
  public void mountSubRouter(String mountPoint, Router subRouter) {
    synchronized (lock) {
      checkNotStopped();
      rootRouter.mountSubRouter(mountPoint, subRouter);
    }
  }

  @Override
  public void removeSubRouter(String mountPoint) {
    synchronized (lock) {
      checkNotStopped();
      rootRouter.getRoutes()
          .stream()
          .filter(r -> r.getPath().equals(mountPoint))
          .forEach(Route::remove);
    }
  }

  @VisibleForTesting
  List<Route> getMountedRoutes() {
    return rootRouter.getRoutes();
  }

  private void checkNotStopped() {
    if (state == STOPPED) {
      throw new IllegalStateException("Server is stopped");
    }
  }

  @Override
  public CompletableFuture<Integer> start(int port) {
    synchronized (lock) {
      if (state != IDLE) {
        throw new IllegalStateException("Cannot start a server when its state is " + state);
      }
      state = STARTED;

      CompletableFuture<Integer> startFuture = new CompletableFuture<>();
      server.listen(port, ar -> handleStartResult(ar, startFuture, port));

      return startFuture;
    }
  }

  private static void handleStartResult(AsyncResult<HttpServer> startResult,
      CompletableFuture<Integer> startFuture, int requestedPort) {
    // Complete the future
    completeFuture(startResult.map(HttpServer::actualPort), startFuture);

    // Log the event
    if (startResult.succeeded()) {
      HttpServer server = startResult.result();
      logger.info("Java server is listening at port {}", server.actualPort());
    } else {
      Throwable failureCause = startResult.cause();
      logger.error("Java server failed to start listening at port {}", requestedPort,
          failureCause);
    }
  }

  @Override
  public OptionalInt getActualPort() {
    synchronized (lock) {
      if (state == STARTED) {
        return OptionalInt.of(server.actualPort());
      } else {
        return OptionalInt.empty();
      }
    }
  }

  @Override
  public CompletableFuture<Void> stop() {
    synchronized (lock) {
      if (stopFuture != null) {
        return stopFuture;
      }

      state = STOPPED;
      stopFuture = new CompletableFuture<>();

      logger.info("Requesting to stop");

      // Request the vertx instance to close itself
      vertx.close(this::notifyVertxStopped);
      return stopFuture;
    }
  }

  private void notifyVertxStopped(AsyncResult<Void> stopResult) {
    logger.info("Stopped");

    synchronized (lock) {
      // Clear the routes when it’s fully stopped
      rootRouter.clear();

      // Notify the clients that the server is stopped
      completeFuture(stopResult, stopFuture);
    }
  }

  private static <R> void completeFuture(AsyncResult<? extends R> result,
      CompletableFuture<? super R> future) {
    if (result.succeeded()) {
      future.complete(result.result());
    } else {
      future.completeExceptionally(result.cause());
    }
  }

  @Override
  public String toString() {
    synchronized (lock) {
      return "Server{"
          + "port=" + server.actualPort()
          + ", state=" + state
          + ", stopFuture=" + stopFuture
          + '}';
    }
  }
}

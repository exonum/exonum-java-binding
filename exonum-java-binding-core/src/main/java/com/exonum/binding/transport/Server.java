package com.exonum.binding.transport;

import io.vertx.ext.web.Router;
import java.util.concurrent.CompletableFuture;

/**
 * An HTTP server providing transport for Exonum transactions and read-requests.
 *
 * @implNote This interface <strong>is</strong> necessary to facilitate testing.
 */
public interface Server {

  /**
   * Creates an HTTP server with no request handlers.
   *
   * <p>Use {@link #start(int)} to start listening to incoming requests.
   */
  static Server create() {
    return new VertxServer();
  }

  /**
   * Creates a request router. The router is empty: it has no routes to request handlers set up.
   *
   * @return a new request router
   */
  Router createRouter();

  /**
   * Mounts the sub router on the root router of this server. You can do that before
   * or after the server has started.
   *
   * <p>Please note that the prefix is stripped from the path when request is forwarded to
   * the sub-router. For example, to handle requests to '/cryptocurrency/send-money'
   * and '/cryptocurrency/balance', use prefix '/cryptocurrency' and a router forwarding
   * requests to '/send-money' and '/balance' to the appropriate handlers.
   *
   * @param mountPoint a mount point (a path prefix) to mount it on
   * @param subRouter a router responsible for handling requests that have the given path prefix
   */
  void mountSubRouter(String mountPoint, Router subRouter);

  /**
   * Starts listening on the given TCP port.
   *
   * @param port a port to listen on
   */
  void start(int port);

  /**
   * Requests the server to stop listening to incoming requests and release any resources.
   * <em>Blocking</em> handlers processing requests will be interrupted.
   *
   * <p>Subsequent invocations have no effect.
   * Once stopped, the server cannot be restarted. Please create a new server.
   *
   * @return a future that is completed when the server is stopped
   */
  CompletableFuture<Void> stop();
}

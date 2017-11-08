package com.exonum.binding.transport;

import static com.exonum.binding.transport.Server.State.IDLE;
import static com.exonum.binding.transport.Server.State.STARTED;
import static com.exonum.binding.transport.Server.State.STOPPED;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * An HTTP server providing transport for Exonum transactions and read-requests.
 *
 * <p>This class hides some nuances of using vert.x as an HTTP server from other components.
 *
 * <p>The class is thread-safe. It does not support client-side locking.
 */
public final class Server {
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
  public Server() {
    vertx = Vertx.vertx();
    rootRouter = Router.router(vertx);
    server = vertx.createHttpServer()
        .requestHandler(rootRouter::accept);
    state = IDLE;
  }

  /**
   * Creates a request router. The router is empty: it has no routes to request handlers set up.
   *
   * @return a new request router
   */
  public Router createRouter() {
    synchronized (lock) {
      checkNotStopped();
      return Router.router(vertx);
    }
  }

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
  public void mountSubRouter(String mountPoint, Router subRouter) {
    synchronized (lock) {
      checkNotStopped();
      rootRouter.mountSubRouter(mountPoint, subRouter);
    }
  }

  private void checkNotStopped() {
    if (state == STOPPED) {
      throw new IllegalStateException("Server is stopped");
    }
  }

  /**
   * Starts listening on the given TCP port.
   *
   * @param port a port to listen on
   */
  public void start(int port) {
    synchronized (lock) {
      if (state != IDLE) {
        throw new IllegalStateException("Cannot start a server when its state is " + state);
      }
      state = STARTED;
      server.listen(port);
    }
  }

  /**
   * Requests the server to stop listening to incoming requests and release any resources.
   * <em>Blocking</em> handlers processing requests will be interrupted.
   *
   * <p>Subsequent invocations have no effect.
   * Once stopped, the server cannot be restarted. Please create a new server.
   *
   * @return a future that is completed when the server is stopped
   */
  public CompletableFuture<Void> stop() {
    synchronized (lock) {
      if (stopFuture != null) {
        return stopFuture;
      }

      state = STOPPED;
      stopFuture = new CompletableFuture<>();

      // Request the vertx instance to close itself
      vertx.close((r) -> {
        // Clear the routes when it's closed
        rootRouter.clear();

        // Notify that the server is stopped
        stopFuture.complete(null);
      });
      return stopFuture;
    }
  }

  @Override
  public String toString() {
    return "Server{"
        + "port=" + server.actualPort()
        + ", state=" + state
        + ", stopFuture=" + stopFuture
        + '}';
  }

  /**
   * A runnable usage sample/playground.
   */
  public static void main(String[] args) {
    Server server = new Server();
    // Create a router of a service
    Router router = server.createRouter();
    router.get("/foo")
        .handler((rc) -> {
          rc.response().end("Hi from /s1/foo");
        });
    router.get("/slow-handler")
        // A terrible idea to sleep in a supposedly non-blocking handler :-) Don't do that.
        // It is NOT interrupted!
        .handler((rc) -> {
          try {
            Thread.sleep(8000);
            rc.response().end("Hi from a terribly slow handler.");
          } catch (InterruptedException e) {
            // Will not happen: Vert.x doesn't interrupt non-blocking handlers.
            System.err.printf("The thread (%s) has been interrupted:%n",
                Thread.currentThread().toString());
            e.printStackTrace();
            Thread.currentThread().interrupt();
          }
        });

    // Mount the service router to a certain path
    server.mountSubRouter("/s1", router);

    server.start(8080);
    System.out.println("Started: " + server);

    // Schedule stopping of the server
    ScheduledExecutorService service = new ScheduledThreadPoolExecutor(1);
    service.schedule(() -> {
      System.out.println("Requesting to stop");

      CompletableFuture<Void> sf = server.stop();

      sf.whenComplete((result, throwable) -> {
        System.out.println("Stopped, stopping the executor");
        service.shutdown();
      });

      System.out.println("…");
    }, 5, TimeUnit.SECONDS);
  }
}

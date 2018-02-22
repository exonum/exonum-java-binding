package com.exonum.binding.transport;

import static com.exonum.binding.transport.VertxServer.State.IDLE;
import static com.exonum.binding.transport.VertxServer.State.STARTED;
import static com.exonum.binding.transport.VertxServer.State.STOPPED;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
  private static final Logger LOG = LogManager.getLogger(VertxServer.class);
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
        .requestHandler(rootRouter::accept);
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

  private void checkNotStopped() {
    if (state == STOPPED) {
      throw new IllegalStateException("Server is stopped");
    }
  }

  @Override
  public void start(int port) {
    synchronized (lock) {
      if (state != IDLE) {
        throw new IllegalStateException("Cannot start a server when its state is " + state);
      }
      state = STARTED;
      server.listen(port);
      LOG.info("Listening at {}", server.actualPort());
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

      LOG.info("Requesting to stop");

      // Request the vertx instance to close itself
      vertx.close((r) -> {
        // Clear the routes when it's closed
        rootRouter.clear();

        // Notify that the server is stopped
        stopFuture.complete(null);

        LOG.info("Stopped");
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
  @SuppressWarnings("FutureReturnValueIgnored")
  public static void main(String[] args) {
    Server server = new VertxServer();
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

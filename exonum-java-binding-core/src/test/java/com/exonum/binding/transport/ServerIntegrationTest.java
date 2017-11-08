package com.exonum.binding.transport;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ServerIntegrationTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final int PORT = 0;

  private Server server;

  @Before
  public void setUp() throws Exception {
    server = new Server();
  }

  @Test
  public void createRouter_stoppedServer() throws Exception {
    blockingStop();

    expectedException.expect(IllegalStateException.class);
    server.createRouter();
  }

  @Test
  public void mountSubRouter_stoppedServer() throws Exception {
    server.start(PORT);
    Router router = server.createRouter();
    blockingStop();

    expectedException.expect(IllegalStateException.class);
    server.mountSubRouter("/service1", router);
  }

  @Test
  public void start_WontStartTwice() throws Exception {
    try {
      server.start(PORT);

      expectedException.expect(IllegalStateException.class);
      server.start(PORT);
    } finally {
      blockingStop();
    }
  }

  @Test
  public void stop_properlyStops() throws Exception {
    server.start(PORT);
    CompletableFuture<Void> f = server.stop();
    f.get(4, TimeUnit.SECONDS);
    assertTrue(f.isDone());
  }

  @Test
  public void stop_subsequentStopsHaveNoEffect() throws Exception {
    server.start(PORT);
    CompletableFuture<Void> f = server.stop();
    f.get(5, TimeUnit.SECONDS);

    CompletableFuture<Void> f2 = server.stop();
    assertTrue(f2.isDone());
  }

  @Test
  public void start_wontStartStopped() throws Exception {
    server.start(PORT);
    blockingStop();

    expectedException.expect(IllegalStateException.class);
    server.start(PORT);
  }

  @Test
  public void start() throws Exception {
    Vertx wcVertx = null;
    try {
      int port = 8080;
      server.start(port);
      Router r = server.createRouter();
      String body = "/s1/foo handler";
      r.get("/foo").handler((rc) -> {
        rc.response().end(body);
      });
      server.mountSubRouter("/s1", r);

      wcVertx = Vertx.vertx();
      WebClient client = WebClient.create(wcVertx);
      // fixme: Async API converted to blocking looks ugly as fuck
      CompletableFuture<AsyncResult<HttpResponse<Buffer>>> f = new CompletableFuture<>();
      client.get(port, "localhost", "/s1/foo")
          .send(f::complete);

      int timeout = 3;
      AsyncResult<HttpResponse<Buffer>> ar = f.get(timeout, TimeUnit.SECONDS);
      assertTrue("Did not receive response in " + timeout + " seconds",
          f.isDone());
      if (ar.succeeded()) {
        HttpResponse<Buffer> response = ar.result();

        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.bodyAsString(), equalTo(body));
      } else {
        fail(ar.cause().getMessage());
      }
    } finally {
      blockingStop();
      if (wcVertx != null) {
        wcVertx.close();
      }
    }
  }

  /**
   * A blocking server stop, so that asynchronous exceptions are not hidden.
   */
  private void blockingStop() throws InterruptedException, ExecutionException, TimeoutException {
    Future<Void> f = server.stop();
    int stopTimeout = 2;
    f.get(stopTimeout, TimeUnit.SECONDS);
  }
}

/*
 * Copyright 2018 The Exonum Team
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

package com.exonum.binding.qaservice;

import static com.google.common.base.Preconditions.checkArgument;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.messages.InternalServerError;
import com.exonum.binding.messages.InvalidTransactionException;
import com.exonum.binding.qaservice.transactions.QaTransactionGson;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(VertxUnitRunner.class)
@SuppressWarnings("WeakerAccess")
public class ApiControllerIntegrationTest {

  private static final String HOST = "0.0.0.0";

  private static final HashCode EXPECTED_TX_HASH = Hashing.sha256()
      .hashInt(1);

  @ClassRule
  public static RunTestOnContext rule = new RunTestOnContext();

  QaService qaService;

  ApiController controller;

  Vertx vertx;

  HttpServer httpServer;

  WebClient webClient;

  volatile int port = -1;

  @Before
  public void setup(TestContext context) {
    qaService = mock(QaService.class);
    controller = new ApiController(qaService);

    vertx = rule.vertx();

    httpServer = vertx.createHttpServer();
    webClient = WebClient.create(vertx);

    Router router = Router.router(vertx);
    controller.mountApi(router);

    Async async = context.async();
    httpServer.requestHandler(router::accept)
        .listen(0, event -> {
          assert event.succeeded();

          // Set the actual server port.
          port = event.result().actualPort();
          // Notify that the HTTP Server is accepting connections.
          async.complete();
        });
  }

  @After
  public void tearDown(TestContext context) {
    Async async = context.async();
    webClient.close();
    httpServer.close(ar -> async.complete());
  }

  @Test
  public void submitCreateCounter(TestContext context) {
    String counterName = "counter 1";
    MultiMap params = multiMap("name", counterName);

    when(qaService.submitCreateCounter(eq(counterName)))
        .thenReturn(EXPECTED_TX_HASH);

    post(ApiController.SUBMIT_CREATE_COUNTER_TX_PATH)
        .sendForm(params, checkCreatedTransaction(context, EXPECTED_TX_HASH));
  }

  @Test
  public void submitCreateCounter_NoParameter(TestContext context) {
    post(ApiController.SUBMIT_CREATE_COUNTER_TX_PATH)
        .sendForm(MultiMap.caseInsensitiveMultiMap(), context.asyncAssertSuccess(ar -> {
          context.verify(v -> {
            assertThat(ar.statusCode()).isEqualTo(HTTP_BAD_REQUEST);

            assertThat(ar.bodyAsString())
                .contains("No required key (name) in request parameters:");
          });
        }));
  }

  @Test
  public void submitCreateCounter_InvalidTransaction(TestContext context) {
    String counterName = "counter 1";
    MultiMap params = multiMap("name", counterName);

    Throwable error = wrappingChecked(InvalidTransactionException.class);
    when(qaService.submitCreateCounter(counterName))
        .thenThrow(error);

    post(ApiController.SUBMIT_CREATE_COUNTER_TX_PATH)
        .sendForm(params, context.asyncAssertSuccess(ar -> {
          context.verify(v -> {
            assertThat(ar.statusCode()).isEqualTo(HTTP_BAD_REQUEST);

            assertThat(ar.bodyAsString())
                .startsWith("Transaction is not valid:");
          });
        }));
  }

  @Test
  public void submitCreateCounter_IllegalArgumentSomewhere(TestContext context) {
    String counterName = "counter 1";
    MultiMap params = multiMap("name", counterName);

    Throwable error = mock(IllegalArgumentException.class);
    when(qaService.submitCreateCounter(counterName))
        .thenThrow(error);

    post(ApiController.SUBMIT_CREATE_COUNTER_TX_PATH)
        .sendForm(params, context.asyncAssertSuccess(ar -> context.verify(v -> {
          assertThat(ar.statusCode()).isEqualTo(HTTP_BAD_REQUEST);
        })));
  }

  @Test
  public void submitCreateCounter_InternalServerError(TestContext context) {
    String counterName = "counter 1";
    MultiMap params = multiMap("name", counterName);

    Throwable error = wrappingChecked(InternalServerError.class);
    when(qaService.submitCreateCounter(counterName))
        .thenThrow(error);

    post(ApiController.SUBMIT_CREATE_COUNTER_TX_PATH)
        .sendForm(params, context.asyncAssertSuccess(ar -> {
          context.verify(v -> {
            assertThat(ar.statusCode()).isEqualTo(HTTP_INTERNAL_ERROR);
          });
        }));
  }

  @Test
  public void submitIncrementCounter(TestContext context) {
    long seed = 1L;
    HashCode counterId = HashCode.fromInt(1);
    MultiMap params = multiMap("seed", Long.toString(seed),
        "counterId", String.valueOf(counterId));

    when(qaService.submitIncrementCounter(eq(seed), eq(counterId)))
        .thenReturn(EXPECTED_TX_HASH);

    post(ApiController.SUBMIT_INCREMENT_COUNTER_TX_PATH)
        .sendForm(params, checkCreatedTransaction(context, EXPECTED_TX_HASH));
  }

  @Test
  public void submitInvalidTx(TestContext context) {
    Throwable error = wrappingChecked(InvalidTransactionException.class);
    when(qaService.submitInvalidTx()).thenThrow(error);

    post(ApiController.SUBMIT_INVALID_TX_PATH)
        .send(checkInvalidTransaction(context));
  }

  @Test
  public void submitInvalidThrowingTx(TestContext context) {
    Throwable error = wrappingChecked(InvalidTransactionException.class);
    when(qaService.submitInvalidThrowingTx()).thenThrow(error);

    post(ApiController.SUBMIT_INVALID_THROWING_TX_PATH)
        .send(checkInvalidTransaction(context));
  }

  @Test
  public void submitValidThrowing(TestContext context) {
    long seed = 10L;

    when(qaService.submitValidThrowingTx(seed))
        .thenReturn(EXPECTED_TX_HASH);

    MultiMap form = multiMap("seed", Long.toString(seed));

    post(ApiController.SUBMIT_VALID_THROWING_TX_PATH)
        .sendForm(form, checkCreatedTransaction(context, EXPECTED_TX_HASH));
  }

  @Test
  public void submitUnknown(TestContext context) {
    when(qaService.submitUnknownTx())
        .thenReturn(EXPECTED_TX_HASH);

    post(ApiController.SUBMIT_UNKNOWN_TX_PATH)
        .send(checkCreatedTransaction(context, EXPECTED_TX_HASH));
  }

  @Test
  public void getCounter(TestContext context) {
    HashCode id = Hashing.sha256().hashInt(2);
    String name = "counter";
    long value = 10L;
    Counter counter = new Counter(name, value);
    when(qaService.getValue(eq(id)))
        .thenReturn(Optional.of(counter));

    String getCounterUri = getCounterUri(id);
    get(getCounterUri)
        .send(context.asyncAssertSuccess(ar -> context.verify(v -> {
          assertThat(ar.statusCode())
              .isEqualTo(HTTP_OK);

          String body = ar.bodyAsString();
          Counter actualCounter = QaTransactionGson.instance()
              .fromJson(body, Counter.class);
          assertThat(actualCounter).isEqualTo(counter);
        })));
  }

  @Test
  public void getCounter_NoCounter(TestContext context) {
    HashCode id = Hashing.sha256().hashInt(2);
    when(qaService.getValue(id))
        .thenReturn(Optional.empty());

    String getCounterUri = getCounterUri(id);
    get(getCounterUri)
        .send(context.asyncAssertSuccess(ar -> context.verify(v -> {
          assertThat(ar.statusCode()).isEqualTo(HTTP_NOT_FOUND);
        })));
  }

  @Test
  public void getCounter_InvalidIdFormat(TestContext context) {
    String hash = "Invalid hexadecimal hash";
    String getCounterUri = getCounterUri(hash);

    get(getCounterUri)
        .send(context.asyncAssertSuccess(ar -> context.verify(v -> {
          assertThat(ar.statusCode()).isEqualTo(HTTP_BAD_REQUEST);
          assertThat(ar.bodyAsString())
              .startsWith("Failed to convert parameter (counterId):");
        })));
  }

  private HttpRequest<Buffer> post(String requestPath) {
    return webClient.post(port, HOST, requestPath);
  }

  private HttpRequest<Buffer> get(String requestPath) {
    return webClient.get(port, HOST, requestPath);
  }

  private static MultiMap multiMap(String k1, String v1, String... entries) {
    checkArgument(entries.length % 2 == 0);

    MultiMap params = MultiMap.caseInsensitiveMultiMap()
        .add(k1, v1);
    int numEntries = entries.length / 2;
    IntStream.range(0, numEntries)
        .forEach(i -> params.add(entries[i], entries[i + 1]));

    return params;
  }

  private String getCounterUri(HashCode id) {
    return getCounterUri(String.valueOf(id));
  }

  private String getCounterUri(String id) {
    try {
      return "/counter/" + URLEncoder.encode(id, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError("UTF-8 must be supported", e);
    }
  }

  private Throwable wrappingChecked(Class<? extends Throwable> checkedException) {
    Throwable wrappingException = logSafeExceptionMock(RuntimeException.class);
    Throwable cause = logSafeExceptionMock(checkedException);
    when(wrappingException.getCause()).thenReturn(cause);
    return wrappingException;
  }

  private Throwable logSafeExceptionMock(Class<? extends Throwable> exceptionType) {
    Throwable t = mock(exceptionType);
    when(t.getStackTrace()).thenReturn(new StackTraceElement[0]);
    return t;
  }

  private Handler<AsyncResult<HttpResponse<Buffer>>> checkCreatedTransaction(
      TestContext context, HashCode expectedTxHash) {
    return context.asyncAssertSuccess(
        ar -> context.verify(v -> {
          assertThat(ar.statusCode()).isEqualTo(HTTP_CREATED);
          assertThat(ar.getHeader("Location"))
              .isEqualTo("/api/explorer/v1/transactions/" + expectedTxHash);
          assertThat(ar.bodyAsString())
              .isEqualTo(expectedTxHash.toString());
        })
    );
  }

  private Handler<AsyncResult<HttpResponse<Buffer>>> checkInvalidTransaction(TestContext context) {
    return context.asyncAssertSuccess(
        ar -> context.verify(v -> {
          assertThat(ar.statusCode()).isEqualTo(HTTP_BAD_REQUEST);
          assertThat(ar.bodyAsString())
              .startsWith("Transaction is not valid");
        })
    );
  }
}

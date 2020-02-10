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

import static com.exonum.binding.common.hash.Hashing.sha256;
import static com.exonum.binding.qaservice.ApiController.QaPaths.GET_CONSENSUS_CONFIGURATION_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.SUBMIT_INCREMENT_COUNTER_TX_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.SUBMIT_UNKNOWN_TX_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.TIME_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.VALIDATORS_TIMES_PATH;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.net.HttpHeaders.LOCATION;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.json.JsonSerializer;
import com.exonum.binding.core.blockchain.serialization.CoreTypeAdapterFactory;
import com.exonum.messages.core.Blockchain.Config;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@Integration
@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
// Execute the tests sequentially, as each of them creates a Vertx instance with its
// own thread pool, which drives the delays up.
@Execution(ExecutionMode.SAME_THREAD)
@SuppressWarnings("WeakerAccess")
class ApiControllerTest {

  private static final String HOST = "0.0.0.0";

  private static final HashCode EXPECTED_TX_HASH = sha256().hashInt(1);

  private static final Gson JSON_SERIALIZER = JsonSerializer.builder()
      .registerTypeAdapterFactory(CoreTypeAdapterFactory.create())
      .create();

  @Mock
  QaService qaService;

  HttpServer httpServer;

  WebClient webClient;

  volatile int port = -1;

  @BeforeEach
  void setup(Vertx vertx, VertxTestContext context) {
    httpServer = vertx.createHttpServer();
    webClient = WebClient.create(vertx);

    Router router = Router.router(vertx);
    new ApiController(qaService).mountApi(router);

    httpServer.requestHandler(router)
        .listen(0, context.succeeding(result -> {

          // Set the actual server port.
          port = result.actualPort();

          context.completeNow();
        }));
  }

  @AfterEach
  void tearDown(VertxTestContext context) {
    webClient.close();
    httpServer.close(context.succeeding(r -> context.completeNow()));
  }

  @Test
  void submitIncrementCounter(VertxTestContext context) {
    long seed = 1L;
    String counterName = "counter1";
    MultiMap params = multiMap("seed", Long.toString(seed),
        "counterName", counterName);

    when(qaService.submitIncrementCounter(eq(seed), eq(counterName)))
        .thenReturn(EXPECTED_TX_HASH);

    post(SUBMIT_INCREMENT_COUNTER_TX_PATH)
        .sendForm(params, checkCreatedTransaction(context, EXPECTED_TX_HASH));
  }

  @Test
  void submitIncrementCounter_NoParameter(VertxTestContext context) {
    // No required seed in the request
    MultiMap params = multiMap("counterName", "num_events");

    post(SUBMIT_INCREMENT_COUNTER_TX_PATH)
        .sendForm(params, context.succeeding(response -> {
          context.verify(() -> {
            assertThat(response.statusCode()).isEqualTo(HTTP_BAD_REQUEST);

            assertThat(response.bodyAsString())
                .contains("No required key (seed) in request parameters:");

            context.completeNow();
          });
        }));
  }

  @Test
  void submitUnknown(VertxTestContext context) {
    when(qaService.submitUnknownTx())
        .thenReturn(EXPECTED_TX_HASH);

    post(SUBMIT_UNKNOWN_TX_PATH)
        .send(checkCreatedTransaction(context, EXPECTED_TX_HASH));
  }

  @Test
  void getCounter(VertxTestContext context) {
    String name = "counter";
    long value = 10L;
    Counter counter = new Counter(name, value);
    when(qaService.getValue(name)).thenReturn(Optional.of(counter));

    String getCounterUri = getCounterUri(name);
    get(getCounterUri)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          Counter actualCounter = JSON_SERIALIZER.fromJson(body, Counter.class);
          assertThat(actualCounter).isEqualTo(counter);

          context.completeNow();
        })));
  }

  @Test
  void getCounter_NoCounter(VertxTestContext context) {
    String name = "counter_1";
    when(qaService.getValue(name))
        .thenReturn(Optional.empty());

    String getCounterUri = getCounterUri(name);
    get(getCounterUri)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode()).isEqualTo(HTTP_NOT_FOUND);

          context.completeNow();
        })));
  }

  @Test
  @DisplayName("failureHandler converts unexpected exceptions to HTTP_INTERNAL_ERROR")
  void failureHandlerUnexpectedException(VertxTestContext context) {
    String id = "Counter_1";
    String message = "Boom";
    // This test is not specific to any service method — what matters is the exception type:
    // RuntimeException.
    when(qaService.getValue(id))
        .thenThrow(new RuntimeException(message));
    String getCounterUri = getCounterUri(id);

    get(getCounterUri)
        .send(context.succeeding(response -> context.verify(() -> {
          assertAll(
              () -> assertThat(response.statusCode()).isEqualTo(HTTP_INTERNAL_ERROR),
              () -> assertThat(response.bodyAsString()).contains(message));
          context.completeNow();
        })));
  }

  @Test
  void multiMapTest() {
    MultiMap m = multiMap("k1", "v1",
        "k2", "v2",
        "k3", "v3");

    assertThat(m.get("k1")).isEqualTo("v1");
    assertThat(m.get("k2")).isEqualTo("v2");
    assertThat(m.get("k3")).isEqualTo("v3");
  }

  @Test
  void getConsensusConfiguration(VertxTestContext context) {
    Config configuration = createConfiguration();
    when(qaService.getConsensusConfiguration()).thenReturn(configuration);

    get(GET_CONSENSUS_CONFIGURATION_PATH)
        .send(context.succeeding(response -> context.verify(() -> {
          assertAll(
              () -> assertThat(response.statusCode()).isEqualTo(HTTP_OK),
              () -> {
                Buffer body = response.bodyAsBuffer();
                Config consensusConfig = Config.parseFrom(body.getBytes());

                assertThat(consensusConfig).isEqualTo(configuration);
              });
          context.completeNow();
        })));
  }

  @Test
  void getTime(VertxTestContext context) {
    ZonedDateTime time = ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    when(qaService.getTime()).thenReturn(Optional.of(time));

    get(TIME_PATH)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          TimeDto actualTime = JSON_SERIALIZER
              .fromJson(body, TimeDto.class);
          assertThat(actualTime.getTime()).isEqualTo(time);

          context.completeNow();
        })));
  }

  @Test
  void getValidatorsTimes(VertxTestContext context) {
    Map<PublicKey, ZonedDateTime> validatorsTimes = ImmutableMap.of(
        PublicKey.fromHexString("11"), ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
        PublicKey.fromHexString("22"), ZonedDateTime.of(2018, 1, 1, 0, 0, 1, 0, ZoneOffset.UTC));
    when(qaService.getValidatorsTimes()).thenReturn(validatorsTimes);

    get(VALIDATORS_TIMES_PATH)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          Map<PublicKey, ZonedDateTime> actualValidatorsTimes = JSON_SERIALIZER
              .fromJson(body, new TypeToken<Map<PublicKey, ZonedDateTime>>() {
              }.getType());
          assertThat(actualValidatorsTimes).isEqualTo(validatorsTimes);

          context.completeNow();
        })));
  }

  private HttpRequest<Buffer> post(String requestPath) {
    return webClient.post(port, HOST, requestPath);
  }

  private HttpRequest<Buffer> get(String requestPath) {
    return webClient.get(port, HOST, requestPath);
  }

  static MultiMap multiMap(String k1, String v1, String... entries) {
    checkArgument(entries.length % 2 == 0);

    MultiMap params = MultiMap.caseInsensitiveMultiMap()
        .add(k1, v1);
    int numEntries = entries.length / 2;
    IntStream.range(0, numEntries)
        .forEach(i -> params.add(entries[2 * i], entries[2 * i + 1]));

    return params;
  }

  private static String getCounterUri(String name) {
    try {
      return "/counter/" + URLEncoder.encode(name, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError("UTF-8 must be supported", e);
    }
  }

  private Handler<AsyncResult<HttpResponse<Buffer>>> checkCreatedTransaction(
      VertxTestContext context, HashCode expectedTxHash) {
    return context.succeeding(
        response -> context.verify(() -> {
          assertAll(
              () -> assertThat(response.bodyAsString()).isEqualTo(expectedTxHash.toString()),
              () -> assertThat(response.statusCode()).isEqualTo(HTTP_CREATED),
              () -> assertThat(response.getHeader(LOCATION))
                  .isEqualTo("/api/explorer/v1/transactions/" + expectedTxHash)
          );
          context.completeNow();
        }));
  }

  private static Config createConfiguration() {
    return Config.newBuilder()
        .setFirstRoundTimeout(1)
        .setStatusTimeout(2)
        .setPeersTimeout(3)
        .setTxsBlockLimit(4)
        .setMaxMessageLen(5)
        .setMinProposeTimeout(6)
        .setMaxProposeTimeout(7)
        .setProposeTimeoutThreshold(8)
        .build();
  }
}

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

package com.exonum.binding.cryptocurrency;

import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.google.gson.reflect.TypeToken;
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
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
// Run all tests in the same thread to avoid creating several Vertx instances, each
// with its own thread pool.
@Execution(ExecutionMode.SAME_THREAD)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class ApiControllerTest {

  private static final String HOST = "0.0.0.0";

  private static final PublicKey FROM_KEY = PredefinedOwnerKeys.FIRST_OWNER_KEY_PAIR.getPublicKey();
  private static final PublicKey TO_KEY = PredefinedOwnerKeys.SECOND_OWNER_KEY_PAIR.getPublicKey();

  @Mock
  private CryptocurrencyService service;

  private HttpServer httpServer;

  private WebClient webClient;

  private volatile int port = -1;

  @BeforeEach
  void setup(Vertx vertx, VertxTestContext context) {
    httpServer = vertx.createHttpServer();
    webClient = WebClient.create(vertx);

    Router router = Router.router(vertx);
    ApiController controller = new ApiController(service);
    controller.mountApi(router);

    httpServer.requestHandler(router)
        .listen(0, context.succeeding(result -> {
          // Set the actual server port.
          port = result.actualPort();
          // Notify that the HTTP Server is accepting connections.
          context.completeNow();
        }));
  }

  @AfterEach
  void tearDown(VertxTestContext context) {
    webClient.close();
    httpServer.close((r) -> context.completeNow());
  }

  @Test
  void getWallet(VertxTestContext context) {
    long balance = 200L;
    Wallet wallet = new Wallet(balance);
    when(service.getWallet(eq(FROM_KEY)))
        .thenReturn(Optional.of(wallet));

    String getWalletUri = getWalletUri(FROM_KEY);
    get(getWalletUri)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          Wallet actualWallet = json()
              .fromJson(body, Wallet.class);
          assertThat(actualWallet.getBalance()).isEqualTo(wallet.getBalance());

          context.completeNow();
        })));
  }

  @Test
  void getNonexistentWallet(VertxTestContext context) {
    when(service.getWallet(FROM_KEY))
        .thenReturn(Optional.empty());

    String getWalletUri = getWalletUri(FROM_KEY);
    get(getWalletUri)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode()).isEqualTo(HTTP_NOT_FOUND);

          context.completeNow();
        })));
  }

  @Test
  void getWalletUsingInvalidKey(VertxTestContext context) {
    String publicKeyString = "Invalid key";
    String getWalletUri = getWalletUri(publicKeyString);

    get(getWalletUri)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode()).isEqualTo(HTTP_BAD_REQUEST);
          assertThat(response.bodyAsString())
              .startsWith("Failed to convert parameter (walletId):");

          context.completeNow();
        })));
  }

  @Test
  void getWalletHistory(VertxTestContext context) {
    List<HistoryEntity> history = singletonList(
        HistoryEntity.newBuilder()
            .setSeed(1L)
            .setWalletFrom(FROM_KEY)
            .setWalletTo(TO_KEY)
            .setAmount(10L)
            .setTxMessageHash(HashCode.fromString("a0a0a0"))
            .build()
    );
    when(service.getWalletHistory(FROM_KEY)).thenReturn(history);

    String uri = getWalletUri(FROM_KEY) + "/history";

    get(uri)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode()).isEqualTo(HTTP_OK);

          List<HistoryEntity> actualHistory = parseWalletHistory(response);

          assertThat(actualHistory).isEqualTo(history);

          context.completeNow();
        })));
  }

  @Test
  void getWalletHistoryNonexistentWallet(VertxTestContext context) {
    when(service.getWalletHistory(FROM_KEY)).thenReturn(emptyList());

    String uri = getWalletUri(FROM_KEY) + "/history";

    get(uri)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode()).isEqualTo(HTTP_OK);
          assertThat(parseWalletHistory(response)).isEmpty();
          context.completeNow();
        })));
  }

  private List<HistoryEntity> parseWalletHistory(HttpResponse<Buffer> response) {
    Type listType = new TypeToken<List<HistoryEntity>>() {
    }.getType();
    return json()
        .fromJson(response.bodyAsString(), listType);
  }

  private String getWalletUri(PublicKey publicKey) {
    return getWalletUri(publicKey.toString());
  }

  private String getWalletUri(String id) {
    try {
      return "/wallet/" + URLEncoder.encode(id, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError("UTF-8 must be supported", e);
    }
  }

  private HttpRequest<Buffer> get(String requestPath) {
    return webClient.get(port, HOST, requestPath);
  }

}

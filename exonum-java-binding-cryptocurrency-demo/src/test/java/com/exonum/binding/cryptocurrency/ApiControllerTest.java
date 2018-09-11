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

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.BinaryMessage;
import com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionGson;
import com.exonum.binding.cryptocurrency.transactions.JsonBinaryMessageConverter;
import com.exonum.binding.service.InternalServerError;
import com.exonum.binding.transaction.Transaction;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class ApiControllerTest {

  private static final String HOST = "0.0.0.0";

  private static final PublicKey fromKey = PredefinedOwnerKeys.firstOwnerKey;

  private CryptocurrencyService service;

  private JsonBinaryMessageConverter jsonBinaryMessageConverter;

  private ApiController controller;

  private HttpServer httpServer;

  private WebClient webClient;

  private volatile int port = -1;

  @BeforeEach
  void setup(Vertx vertx, VertxTestContext context) {
    service = mock(CryptocurrencyService.class);
    jsonBinaryMessageConverter = mock(JsonBinaryMessageConverter.class);
    controller = new ApiController(service, jsonBinaryMessageConverter);

    httpServer = vertx.createHttpServer();
    webClient = WebClient.create(vertx);

    Router router = Router.router(vertx);
    controller.mountApi(router);

    httpServer.requestHandler(router::accept)
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
  void submitValidTransaction(VertxTestContext context) {
    String messageJson = "{\"service_id\":42}";
    String messageHash = "1234";
    BinaryMessage message = mock(BinaryMessage.class);
    Transaction transaction = mock(Transaction.class);

    when(jsonBinaryMessageConverter.toMessage(messageJson))
        .thenReturn(message);
    when(service.convertToTransaction(message))
        .thenReturn(transaction);
    when(service.submitTransaction(transaction))
        .thenReturn(HashCode.fromString(messageHash));

    String expectedResponse = messageHash;
    // Send a request to submitTransaction
    post(ApiController.SUBMIT_TRANSACTION_PATH)
        .sendJsonObject(
            new JsonObject(messageJson),
            context.succeeding(
                response -> context.verify(() -> {
                  // Check the response status
                  int statusCode = response.statusCode();
                  assertEquals(HTTP_OK, statusCode);

                  // Check the payload type
                  String contentType = response.getHeader("Content-Type");
                  assertEquals("text/plain", contentType);

                  // Check the response body
                  String body = response.bodyAsString();
                  assertEquals(expectedResponse, body);

                  // Verify that a proper transaction was submitted to the network
                  verify(service).submitTransaction(transaction);

                  context.completeNow();
                })));
  }

  @Test
  void submitTransactionWhenInternalServerErrorIsThrown(VertxTestContext context) {
    String messageJson = "{\"service_id\":42}";
    BinaryMessage message = mock(BinaryMessage.class);
    Transaction transaction = mock(Transaction.class);
    Throwable error = wrappingChecked(InternalServerError.class);

    when(jsonBinaryMessageConverter.toMessage(messageJson))
        .thenReturn(message);
    when(service.convertToTransaction(message))
        .thenReturn(transaction);
    when(service.submitTransaction(transaction))
        .thenThrow(error);

    post(ApiController.SUBMIT_TRANSACTION_PATH)
        .sendJsonObject(
            new JsonObject(messageJson),
            context.succeeding(response -> context.verify(() -> {
              assertThat(response.statusCode()).isEqualTo(HTTP_INTERNAL_ERROR);
              verify(service).submitTransaction(transaction);

              context.completeNow();
            })));
  }

  @Test
  void getWallet(VertxTestContext context) {
    long balance = 200L;
    Wallet wallet = new Wallet(balance);
    when(service.getWallet(eq(fromKey)))
        .thenReturn(Optional.of(wallet));

    String getWalletUri = getWalletUri(fromKey);
    get(getWalletUri)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          Wallet actualWallet = CryptocurrencyTransactionGson.instance()
              .fromJson(body, Wallet.class);
          assertThat(actualWallet.getBalance()).isEqualTo(wallet.getBalance());

          context.completeNow();
        })));
  }

  @Test
  void getNonexistentWallet(VertxTestContext context) {
    when(service.getWallet(fromKey))
        .thenReturn(Optional.empty());

    String getWalletUri = getWalletUri(fromKey);
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

  private Throwable wrappingChecked(Class<? extends Throwable> checkedException) {
    Throwable wrappingException = logSafeExceptionMock(RuntimeException.class);
    Throwable cause = logSafeExceptionMock(checkedException);
    when(wrappingException.getCause()).thenReturn(cause);
    return wrappingException;
  }

  private Throwable logSafeExceptionMock(Class<? extends Throwable> exceptionType) {
    Throwable t = mock(exceptionType);
    return t;
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

  private HttpRequest<Buffer> post(String requestPath) {
    return webClient.post(port, HOST, requestPath);
  }
}

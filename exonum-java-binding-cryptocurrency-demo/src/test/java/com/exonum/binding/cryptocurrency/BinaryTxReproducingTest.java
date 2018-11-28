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

import static java.net.HttpURLConnection.HTTP_OK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.message.BinaryMessage;
import com.exonum.binding.cryptocurrency.transactions.CreateWalletTransactionUtils;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(VertxExtension.class)
class BinaryTxReproducingTest {

  private static final String HOST = "127.0.0.1";
  private static final int PORT = 6040;
  private static final String SUBMIT_TX_REQUEST_PATH =
      "/api/cryptocurrency-demo-service/" + ApiController.SUBMIT_TRANSACTION_PATH;

  private WebClient webClient;

  @BeforeAll
  void setUp(Vertx vertx) {
    webClient = WebClient.create(vertx);
  }

  @AfterAll
  void tearDown() {
    webClient.close();
  }

  @ParameterizedTest
  @MethodSource("variousValidTransactions2")
  void submitVariousValidTransactionsUntilControllerFails(
      BinaryMessage txMessage, VertxTestContext context) {

    // Setup test
    HashCode messageHash = txMessage.hash();

    // Submit the message
    post(SUBMIT_TX_REQUEST_PATH)
        .putHeader("Content-Type", "application/x-www-form-urlencoded") // <-- Will fail some transactions
        .sendBuffer(
            Buffer.buffer(txMessage.getSignedMessage().array()),
            context.succeeding(
                response -> context.verify(() -> {
                  // Verify the test results (all must be OK)
                  assertAll(
                      () -> {
                        // Check the response status
                        int statusCode = response.statusCode();
                        assertEquals(HTTP_OK, statusCode);
                      },

                      () -> {
                        // Check the payload type
                        String contentType = response.getHeader("Content-Type");
                        assertEquals("text/plain", contentType);
                      },

                      () -> {
                        // Check the response body
                        String body = response.bodyAsString();
                        assertEquals(messageHash.toString(), body);
                      }
                  );

                  context.completeNow();
                })));
  }

  @SuppressWarnings("unused")
  private static Stream<BinaryMessage> variousValidTransactions() {
    KeyPair ownerKeyPair = CryptoFunctions.ed25519().generateKeyPair(
        Hashing.sha256().hashString("seed", UTF_8).asBytes()
    );
    int balanceRangeStart = 100000;
    int balanceRangeEnd = balanceRangeStart + 1024;

    return IntStream.range(balanceRangeStart, balanceRangeEnd)
        .mapToObj(balance -> CreateWalletTransactionUtils.createUnsignedMessage(
            ownerKeyPair.getPublicKey(), balance))
        .map(unsigned -> unsigned.sign(CryptoFunctions.ed25519(), ownerKeyPair.getPrivateKey()));
  }

  @SuppressWarnings("unused")
  private static Stream<BinaryMessage> variousValidTransactions2() {
    int numMessages = 16 * 1024;

    return IntStream.range(0, numMessages)
        // Each gets its own random key pair
        .mapToObj(i -> CryptoFunctions.ed25519().generateKeyPair())
        .map((ownerKeyPair) -> CreateWalletTransactionUtils.createUnsignedMessage(
            ownerKeyPair.getPublicKey(), 100500)
            .sign(CryptoFunctions.ed25519(), ownerKeyPair.getPrivateKey()));
  }

  private HttpRequest<Buffer> post(String requestPath) {
    return webClient.post(PORT, HOST, requestPath);
  }
}

/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.cryptocurrency;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.cryptocurrency.transactions.CreateWalletTx;
import com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransaction;
import com.exonum.binding.cryptocurrency.transactions.TransferTx;
import com.exonum.binding.messages.InternalServerError;
import com.exonum.binding.messages.InvalidTransactionException;
import com.exonum.binding.messages.Transaction;
import com.google.common.collect.ImmutableMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import java.net.HttpURLConnection;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ApiControllerTest {

  private static final String HOST = "0.0.0.0";

  private static final PublicKey fromKey = PredefinedOwnerKeys.firstOwnerKey;

  private static final PublicKey toKey = PredefinedOwnerKeys.secondOwnerKey;

  @ClassRule public static RunTestOnContext rule = new RunTestOnContext();

  CryptocurrencyService service;

  ApiController controller;

  Vertx vertx;

  HttpServer httpServer;

  WebClient webClient;

  @Before
  public void setup(TestContext context) {
    service = mock(CryptocurrencyService.class);
    controller = new ApiController(service);

    vertx = rule.vertx();

    httpServer = vertx.createHttpServer();
    webClient = WebClient.create(vertx);

    Router router = Router.router(vertx);
    controller.mountApi(router);

    Async async = context.async();
    httpServer.requestHandler(router::accept).listen(0, event -> async.complete());
  }

  @After
  public void tearDown(TestContext context) {
    Async async = context.async();
    webClient.close();
    httpServer.close((ar) -> async.complete());
  }

  @Test
  public void handlesAllKnownTransactions(TestContext context) {
    Map<CryptocurrencyTransaction, Transaction> transactionTemplates =
        ImmutableMap.of(
            CryptocurrencyTransaction.CREATE_WALLET,
                new CreateWalletTx(fromKey),
            CryptocurrencyTransaction.TRANSFER,
                new TransferTx(
                    0L,
                    fromKey,
                    toKey,
                    40L));

    int port = httpServer.actualPort();
    for (Map.Entry<CryptocurrencyTransaction, Transaction> entry :
        transactionTemplates.entrySet()) {
      Transaction sourceTx = entry.getValue();
      String expectedResponse = String.valueOf(sourceTx.hash());

      String sourceTxMessage = sourceTx.info();

      try {
        when(service.submitTransaction(eq(sourceTx)))
            .thenReturn(sourceTx.hash());
      } catch (InvalidTransactionException | InternalServerError e) {
        throw new AssertionError(e);
      }

      // Send a request to submitTransaction
      webClient
          .post(port, HOST, ApiController.SUBMIT_TRANSACTION_PATH)
          .sendJsonObject(
              new JsonObject(sourceTxMessage),
              context.asyncAssertSuccess(
                  r -> {

                    // Check the response status
                    int statusCode = r.statusCode();
                    context.assertEquals(statusCode, HttpURLConnection.HTTP_OK);

                    // Check the response body
                    String response = r.bodyAsString();
                    context.assertEquals(response, expectedResponse);

                    try {
                      // Verify that a proper transaction was submitted to the network
                      verify(service).submitTransaction(eq(sourceTx));
                    } catch (InvalidTransactionException | InternalServerError e) {
                      throw new AssertionError(e);
                    }
                  }));
    }
  }

  @Test
  public void serverErrorOnError(TestContext context) throws Exception {
    int port = httpServer.actualPort();
    PublicKey publicKey = fromKey;
    Transaction tx = new CreateWalletTx(publicKey);
    String txMessageJson = tx.info();

    doThrow(InternalServerError.class).when(service).submitTransaction(any(Transaction.class));

    // Send a request to submitTransaction
    webClient
        .post(port, HOST, ApiController.SUBMIT_TRANSACTION_PATH)
        .sendJsonObject(
            new JsonObject(txMessageJson),
            context.asyncAssertSuccess(
                r -> {

                  // Check the response status
                  int statusCode = r.statusCode();
                  context.assertEquals(statusCode, HttpURLConnection.HTTP_INTERNAL_ERROR);

                  try {
                    // Verify that transaction was attempted to be submitted to the network
                    verify(service).submitTransaction(any());
                  } catch (InvalidTransactionException | InternalServerError e) {
                    throw new AssertionError("Unexpected exception", e);
                  }
                }));
  }
}

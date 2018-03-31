package com.exonum.binding.cryptocurrency;

import com.exonum.binding.cryptocurrency.transactions.CreateWalletTx;
import com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransaction;
import com.exonum.binding.cryptocurrency.transactions.TransferTx;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.messages.InternalServerError;
import com.exonum.binding.messages.InvalidTransactionException;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.service.Node;
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
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(VertxUnitRunner.class)
public class ApiControllerTest {

  private static final String HOST = "0.0.0.0";

  @ClassRule public static RunTestOnContext rule = new RunTestOnContext();

  Node node;

  ApiController controller;

  Vertx vertx;

  HttpServer httpServer;

  WebClient webClient;

  @Before
  public void setup(TestContext context) {
    node = mock(Node.class);
    controller = new ApiController(node);

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
            CryptocurrencyTransaction.CREATE_WALLET, new CreateWalletTx("wallet_name"),
            CryptocurrencyTransaction.TRANSFER,
                new TransferTx(
                    0L,
                    Hashing.defaultHashFunction().hashString("from", UTF_8),
                    Hashing.defaultHashFunction().hashString("to", UTF_8),
                    40L));

    int port = httpServer.actualPort();
    for (Map.Entry<CryptocurrencyTransaction, Transaction> entry :
        transactionTemplates.entrySet()) {
      Transaction sourceTx = entry.getValue();
      String expectedResponse = String.valueOf(sourceTx.hash());

      String sourceTxMessage = sourceTx.info();

      // Send a request to submitTransaction
      webClient
          .post(port, HOST, ApiController.SUBMIT_TRANSACTION_PATH)
          //                    .putHeader("content-type", "application/json")
          .sendJsonObject(
              new JsonObject(sourceTxMessage),
              context.asyncAssertSuccess(
                  v -> {

                    // Check the response status
                    int statusCode = v.statusCode();
                    assertThat(statusCode).isEqualTo(HttpURLConnection.HTTP_OK);

                    // Check the response body
                    String response = v.bodyAsString();
                    assertThat(response).isEqualTo(expectedResponse);

                    try {
                      // Verify that a proper transaction was submitted to the network
                      verify(node).submitTransaction(any(sourceTx.getClass()));
                      //                            async.complete();
                    } catch (InvalidTransactionException | InternalServerError e) {
                      throw new AssertionError(e);
                    }
                  }));
    }
  }

  @Test
  public void serverErrorOnError(TestContext context) throws Exception {
    int port = httpServer.actualPort();
    Transaction tx = new CreateWalletTx("new-wallet");
    String txMessageJson = tx.info();

    doThrow(InternalServerError.class).when(node).submitTransaction(any(Transaction.class));

    // Send a request to submitTransaction
    webClient
        .post(port, HOST, ApiController.SUBMIT_TRANSACTION_PATH)
        .putHeader("content-type", "application/json")
        .sendJsonObject(
            new JsonObject(txMessageJson),
            context.asyncAssertSuccess(
                v -> {

                  // Check the response status
                  int statusCode = v.statusCode();
                  assertThat(statusCode).isEqualTo(500);

                  try {
                    // Verify that transaction was attempted to be submitted to the network
                    verify(node).submitTransaction(any());
                  } catch (InvalidTransactionException | InternalServerError e) {
                    throw new AssertionError("Unexpected exception", e);
                  }
                }));
  }
}

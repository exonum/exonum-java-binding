package com.exonum.binding.qaservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.exonum.binding.hash.Hashing;
import com.exonum.binding.messages.InternalServerError;
import com.exonum.binding.messages.InvalidTransactionException;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.qaservice.transactions.AnyTransaction;
import com.exonum.binding.qaservice.transactions.CreateCounterTx;
import com.exonum.binding.qaservice.transactions.IncrementCounterTx;
import com.exonum.binding.qaservice.transactions.InvalidThrowingTx;
import com.exonum.binding.qaservice.transactions.InvalidTx;
import com.exonum.binding.qaservice.transactions.QaTransaction;
import com.exonum.binding.qaservice.transactions.QaTransactionGson;
import com.exonum.binding.qaservice.transactions.ValidThrowingTx;
import com.exonum.binding.service.Node;
import com.google.common.collect.ImmutableMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ApiController2ExperimentalIntegrationTest {

  private static final String HOST = "0.0.0.0";

  @ClassRule
  public static RunTestOnContext rule = new RunTestOnContext();

  Node node;

  ApiController2 controller;

  Vertx vertx;

  HttpServer httpServer;

  WebClient webClient;

  @Before
  public void setup(TestContext context) {
    node = mock(Node.class);
    controller = new ApiController2(node);

    vertx = rule.vertx();

    httpServer = vertx.createHttpServer();
    webClient = WebClient.create(vertx);

    Router router = Router.router(vertx);
    controller.mountApi(router);

    Async async = context.async();
    httpServer.requestHandler(router::accept)
        .listen(0, event -> async.complete());
  }

  @After
  public void tearDown(TestContext context) {
    Async async = context.async();
    webClient.close();
    httpServer.close((ar) -> async.complete());
  }

  @Test
  public void handlesAllKnownTransactions(TestContext context) {
    Map<QaTransaction, Transaction> transactionTemplates = ImmutableMap.of(
        QaTransaction.CREATE_COUNTER, new CreateCounterTx("counter_name"),
        QaTransaction.INCREMENT_COUNTER, new IncrementCounterTx(0L, Hashing.sha256().hashInt(0)),
        QaTransaction.INVALID, new InvalidTx(),
        QaTransaction.INVALID_THROWING, new InvalidThrowingTx(),
        QaTransaction.VALID_THROWING, new ValidThrowingTx(0L)
    );

    // Check that the templates above are correct
    context.verify((v) -> assertThat(transactionTemplates)
        .hasSameSizeAs(QaTransaction.values()));

    int port = httpServer.actualPort();
    for (Map.Entry<QaTransaction, Transaction> entry : transactionTemplates.entrySet()) {
      Transaction sourceTx = entry.getValue();
      String expectedResponse = String.valueOf(sourceTx.hash());

      String sourceTxMessage = sourceTx.info();

      Async async = context.async();
      // Send a request to submitTransaction
      webClient.post(port, HOST, ApiController2.SUBMIT_TRANSACTION_PATH)
          .putHeader("content-type", "application/json")
          .sendBuffer(Buffer.buffer(sourceTxMessage), ar -> context.verify(v -> {
            assertThat(ar.succeeded())
                .as("Response to %s: %s", sourceTx, ar)
                .isTrue();

            // Check the response status
            HttpResponse<Buffer> result = ar.result();
            int statusCode = result.statusCode();
            assertThat(statusCode).isEqualTo(200);

            // Check the response body
            String response = result.bodyAsString();
            assertThat(response).isEqualTo(expectedResponse);

            try {
              // Verify that a proper transaction was submitted to the network
              verify(node).submitTransaction(any(sourceTx.getClass()));
              System.out.println("Test for " + sourceTx + " passed!");
              async.complete();
            } catch (InvalidTransactionException | InternalServerError e) {
              throw new AssertionError(e);
            }
          }));
    }
  }

  @Test
  public void rejectsTransactionOfAnotherService(TestContext context) {
    int port = httpServer.actualPort();
    AnyTransaction<Map<String, String>> unknownTxMessage = new AnyTransaction<>(
        (short) (QaService.ID + 1),
        QaTransaction.CREATE_COUNTER.id(),
        ImmutableMap.of("seed", "1")
    );
    String unknownTxMessageJson = QaTransactionGson.instance().toJson(unknownTxMessage);
    Async async = context.async();
    // Send a request to submitTransaction
    webClient.post(port, HOST, ApiController2.SUBMIT_TRANSACTION_PATH)
        .putHeader("content-type", "application/json")
        .sendBuffer(Buffer.buffer(unknownTxMessageJson), ar -> context.verify(v -> {
          assertThat(ar.succeeded())
              .as("Response: %s", ar)
              .isTrue();

          // Check the response status
          HttpResponse<Buffer> result = ar.result();
          int statusCode = result.statusCode();
          assertThat(statusCode).isEqualTo(400);

          // Check the response body
          String response = result.bodyAsString();
          String expectedResponse = "Unknown service id \\(\\d+\\), must be \\(" + QaService.ID + "\\)";
          assertThat(response).matches(expectedResponse);

          try {
            // Verify that no transaction was submitted to the network
            verify(node, never()).submitTransaction(any(Transaction.class));
            async.complete();
          } catch (InvalidTransactionException | InternalServerError e) {
            throw new AssertionError("Unexpected exception", e);
          }
        }));
  }

  @Test
  public void rejectsUnknownTransactionOfThisService(TestContext context) {
    int port = httpServer.actualPort();
    AnyTransaction<Map<String, String>> unknownTxMessage = new AnyTransaction<>(
        QaService.ID,
        (short) 9999,
        ImmutableMap.of("seed", "1")
    );
    String unknownTxMessageJson = QaTransactionGson.instance().toJson(unknownTxMessage);
    Async async = context.async();
    // Send a request to submitTransaction
    webClient.post(port, HOST, ApiController2.SUBMIT_TRANSACTION_PATH)
        .putHeader("content-type", "application/json")
        .sendBuffer(Buffer.buffer(unknownTxMessageJson), ar -> context.verify(v -> {
          assertThat(ar.succeeded())
              .as("Response: %s", ar)
              .isTrue();

          // Check the response status
          HttpResponse<Buffer> result = ar.result();
          int statusCode = result.statusCode();
          assertThat(statusCode).isEqualTo(400);

          // Check the response body
          String response = result.bodyAsString();
          String expectedResponse = "Unknown transaction id \\(\\d+\\)";
          assertThat(response).matches(expectedResponse);

          try {
            // Verify that no transaction was submitted to the network
            verify(node, never()).submitTransaction(any());
            async.complete();
          } catch (InvalidTransactionException | InternalServerError e) {
            throw new AssertionError("Unexpected exception", e);
          }
        }));
  }

  @Test
  public void badRequestOnInvalidTransaction(TestContext context) throws InvalidTransactionException, InternalServerError {
    int port = httpServer.actualPort();
    InvalidTx tx = new InvalidTx();
    String txMessageJson = tx.info();

    doThrow(InvalidTransactionException.class)
        .when(node).submitTransaction(any(InvalidTx.class));

    Async async = context.async();
    // Send a request to submitTransaction
    webClient.post(port, HOST, ApiController2.SUBMIT_TRANSACTION_PATH)
        .putHeader("content-type", "application/json")
        .sendBuffer(Buffer.buffer(txMessageJson), ar -> context.verify(v -> {
          assertThat(ar.succeeded())
              .as("Response: %s", ar)
              .isTrue();

          // Check the response status
          HttpResponse<Buffer> result = ar.result();
          int statusCode = result.statusCode();
          assertThat(statusCode).isEqualTo(400);

          // Check the response status message
          assertThat(result.statusMessage())
              .isEqualTo("Bad Request: transaction is not valid");

          try {
            // Verify that transaction was attempted to be submitted to the network
            verify(node).submitTransaction(any(InvalidTx.class));
            async.complete();
          } catch (InvalidTransactionException | InternalServerError e) {
            throw new AssertionError("Unexpected exception", e);
          }
        }));
  }

  @Test
  public void serverErrorOnError(TestContext context) throws InvalidTransactionException, InternalServerError {
    int port = httpServer.actualPort();
    Transaction tx = new CreateCounterTx("new-counter");
    String txMessageJson = tx.info();

    doThrow(InternalServerError.class)
        .when(node).submitTransaction(any(Transaction.class));

    Async async = context.async();
    // Send a request to submitTransaction
    webClient.post(port, HOST, ApiController2.SUBMIT_TRANSACTION_PATH)
        .putHeader("content-type", "application/json")
        .sendBuffer(Buffer.buffer(txMessageJson), ar -> context.verify(v -> {
          assertThat(ar.succeeded())
              .as("Response: %s", ar)
              .isTrue();

          // Check the response status
          HttpResponse<Buffer> result = ar.result();
          int statusCode = result.statusCode();
          assertThat(statusCode).isEqualTo(500);

          try {
            // Verify that transaction was attempted to be submitted to the network
            verify(node).submitTransaction(any());
            async.complete();
          } catch (InvalidTransactionException | InternalServerError e) {
            throw new AssertionError("Unexpected exception", e);
          }
        }));
  }

  // todo: add invalid transactions (not a valid json, etc.)
}

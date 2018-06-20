package com.exonum.binding.cryptocurrency;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.cryptocurrency.transactions.CreateWalletTx;
import com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransaction;
import com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionGson;
import com.exonum.binding.cryptocurrency.transactions.TransferTx;
import com.exonum.binding.messages.InternalServerError;
import com.exonum.binding.messages.Transaction;
import com.google.common.collect.ImmutableMap;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Optional;
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

  volatile int port = -1;

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

    for (Map.Entry<CryptocurrencyTransaction, Transaction> entry :
        transactionTemplates.entrySet()) {
      Transaction sourceTx = entry.getValue();
      String expectedResponse = String.valueOf(sourceTx.hash());

      String sourceTxMessage = sourceTx.info();

      when(service.submitTransaction(eq(sourceTx)))
          .thenReturn(sourceTx.hash());

      // Send a request to submitTransaction
      post(ApiController.SUBMIT_TRANSACTION_PATH)
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

                    // Verify that a proper transaction was submitted to the network
                    verify(service).submitTransaction(eq(sourceTx));
                  }));
    }
  }

  @Test
  public void getWallet(TestContext context) {
    long balance = 200L;
    Wallet wallet = new Wallet(balance);
    when(service.getValue(eq(fromKey)))
        .thenReturn(Optional.of(wallet));

    String getWalletUri = getWalletUri(fromKey);
    get(getWalletUri)
        .send(context.asyncAssertSuccess(ar -> context.verify(v -> {
          assertThat(ar.statusCode())
              .isEqualTo(HTTP_OK);

          String body = ar.bodyAsString();
          Wallet actualWallet = CryptocurrencyTransactionGson.instance()
              .fromJson(body, Wallet.class);
          assertThat(actualWallet.getBalance()).isEqualTo(wallet.getBalance());
        })));
  }

  @Test
  public void getNonexistentWallet(TestContext context) {
    when(service.getValue(fromKey))
        .thenReturn(Optional.empty());

    String getWalletUri = getWalletUri(fromKey);
    get(getWalletUri)
        .send(context.asyncAssertSuccess(ar -> context.verify(v -> {
          assertThat(ar.statusCode()).isEqualTo(HTTP_NOT_FOUND);
        })));
  }

  @Test
  public void getWalletUsingInvalidKey(TestContext context) {
    String publicKeyString = "Invalid key";
    String getWalletUri = getWalletUri(publicKeyString);

    get(getWalletUri)
        .send(context.asyncAssertSuccess(ar -> context.verify(v -> {
          assertThat(ar.statusCode()).isEqualTo(HTTP_BAD_REQUEST);
          assertThat(ar.bodyAsString())
              .startsWith("Failed to convert parameter (walletId):");
        })));
  }

  @Test
  public void submitTransactionWhenInternalServerErrorIsThrown(TestContext context) {
    Throwable error = wrappingChecked(InternalServerError.class);

    Transaction transaction = new CreateWalletTx(fromKey);
    when(service.submitTransaction(eq(transaction)))
        .thenThrow(error);
    String sourceTxMessage = transaction.info();

    post(ApiController.SUBMIT_TRANSACTION_PATH)
        .sendJsonObject(
            new JsonObject(sourceTxMessage),
            context.asyncAssertSuccess(ar -> {
              context.verify(v -> {
                assertThat(ar.statusCode()).isEqualTo(HTTP_INTERNAL_ERROR);
              });
            }));
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

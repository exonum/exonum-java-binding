package com.exonum.binding.cryptocurrency;

import static com.google.common.base.Preconditions.checkArgument;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.cryptocurrency.transactions.BaseTx;
import com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransaction;
import com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionGson;
import com.exonum.binding.hash.HashCode;
import com.exonum.binding.messages.InvalidTransactionException;
import com.exonum.binding.messages.Transaction;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Controller for submitting transactions. */
final class ApiController {

  private static final Logger logger = LogManager.getLogger(ApiController.class);

  @VisibleForTesting static final String SUBMIT_TRANSACTION_PATH = "/submit-transaction";
  private static final String WALLET_ID_PARAM = "walletId";
  private static final String GET_WALLET_PATH = "/wallet/:" + WALLET_ID_PARAM;

  private static final Map<Short, Type> TX_MESSAGE_TYPES =
      Arrays.stream(CryptocurrencyTransaction.values())
          .collect(
              Collectors.toMap(
                  CryptocurrencyTransaction::getId, CryptocurrencyTransaction::transactionClass));

  private final CryptocurrencyService service;

  @Inject
  ApiController(CryptocurrencyService service) {
    this.service = service;
  }

  void mountApi(Router router) {
    router.route()
        .handler(BodyHandler.create())
        .failureHandler(this::failureHandler);

    ImmutableMap<String, Handler<RoutingContext>> handlers =
        ImmutableMap.<String, Handler<RoutingContext>>builder()
            .put(SUBMIT_TRANSACTION_PATH, this::submitTransaction)
            .put(GET_WALLET_PATH, this::getWallet)
            .build();

    handlers.forEach((path, handler) ->
        router.route(path).handler(handler)
    );
  }

  private void submitTransaction(RoutingContext rc) {
    Gson gson = CryptocurrencyTransactionGson.instance();
    // Extract the transaction type from the body
    String body = rc.getBodyAsString();
    BaseTx rawTxMessage = gson.fromJson(body, BaseTx.class);
    short serviceId = rawTxMessage.getServiceId();
    if (serviceId != CryptocurrencyService.ID) {
      rc.response()
          .setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST)
          .end(
              String.format(
                  "Unknown service ID (%d), must be (%d)",
                  rawTxMessage.getServiceId(), CryptocurrencyService.ID));
      return;
    }

    short txId = rawTxMessage.getMessageId();
    Optional<Transaction> maybeTx = txFromMessage(rc, txId, body);
    if (!maybeTx.isPresent()) {
      return;
    }
    Transaction tx = maybeTx.get();

    HashCode txHash = service.submitTransaction(tx);
    rc.response().end(String.valueOf(txHash));
  }

  private void getWallet(RoutingContext rc) {
    PublicKey walletId =
        getRequiredParameter(rc.request(), WALLET_ID_PARAM, PublicKey::fromHexString);

    Optional<Wallet> wallet = service.getValue(walletId);

    if (wallet.isPresent()) {
      Gson gson = CryptocurrencyTransactionGson.instance();
      rc.response()
          .putHeader("Content-Type", "application/json")
          .end(gson.toJson(wallet.get()));
    } else {
      rc.response()
          .setStatusCode(HTTP_NOT_FOUND)
          .end();
    }
  }

  private static <T> T getRequiredParameter(HttpServerRequest request, String key,
      Function<String, T> converter) {
    return getRequiredParameter(request.params(), key, converter);
  }

  private static <T> T getRequiredParameter(MultiMap parameters, String key,
      Function<String, T> converter) {
    checkArgument(parameters.contains(key), "No required key (%s) in request parameters: %s",
        key, parameters);
    String parameter = parameters.get(key);
    try {
      return converter.apply(parameter);
    } catch (Exception e) {
      String message = String.format("Failed to convert parameter (%s): %s", key, e.getMessage());
      throw new IllegalArgumentException(message);
    }
  }

  private void failureHandler(RoutingContext rc) {
    logger.info("An error whilst processing request {}", rc.normalisedPath());

    Throwable requestFailure = rc.failure();
    if (requestFailure != null) {
      Optional<String> badRequest = badRequestDescription(requestFailure);
      if (badRequest.isPresent()) {
        rc.response()
            .setStatusCode(HTTP_BAD_REQUEST)
            .end(badRequest.get());
      } else {
        logger.error("Request error:", requestFailure);
        rc.response()
            .setStatusCode(HTTP_INTERNAL_ERROR)
            .end();
      }
    } else {
      int failureStatusCode = rc.statusCode();
      rc.response()
          .setStatusCode(failureStatusCode)
          .end();
    }
  }

  /**
   * If the passed throwable corresponds to a bad request — returns an error message,
   * or {@code Optional.empty()} otherwise.
   */
  private Optional<String> badRequestDescription(Throwable requestFailure) {
    // All IllegalArgumentExceptions are considered to be caused by a bad request.
    if (requestFailure instanceof IllegalArgumentException) {
      String message = Strings.nullToEmpty(requestFailure.getLocalizedMessage());
      return Optional.of(message);
    }

    // Check for checked InvalidTransactionExceptions — they are wrapped in RuntimeExceptions.
    Throwable cause = requestFailure.getCause();
    if (cause instanceof InvalidTransactionException) {
      String description = Strings.nullToEmpty(cause.getLocalizedMessage());
      String message = String.format("Transaction is not valid: %s", description);
      return Optional.of(message);
    }
    // This throwable must correspond to an internal server error.
    return Optional.empty();
  }

  private Optional<Transaction> txFromMessage(RoutingContext rc, short txId, String txMessageJson) {
    if (!TX_MESSAGE_TYPES.containsKey(txId)) {
      rc.response()
          .setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST)
          .end(String.format("Unknown transaction ID (%d)", txId));
      return Optional.empty();
    }
    Gson gson = CryptocurrencyTransactionGson.instance();
    Type txMessageType = TX_MESSAGE_TYPES.get(txId);
    try {
      Transaction txMessage = gson.fromJson(txMessageJson, txMessageType);
      return Optional.of(txMessage);
    } catch (JsonSyntaxException e) {
      throw new AssertionError("JSON must have been already validated");
    } catch (JsonParseException e) {
      rc.response()
          .setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST)
          .end(
              String.format(
                  "Cannot convert transaction (%d) into an instance "
                      + "of the corresponding class (%s)",
                  txId, txMessageType));
      return Optional.empty();
    }
  }
}

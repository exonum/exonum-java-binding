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

package com.exonum.binding.qaservice;

import static com.google.common.base.Preconditions.checkArgument;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.messages.InvalidTransactionException;
import com.exonum.binding.qaservice.transactions.QaTransactionGson;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.inject.Inject;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.Optional;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class ApiController {

  @VisibleForTesting
  static final String SUBMIT_CREATE_COUNTER_TX_PATH = "/submit-create-counter";
  @VisibleForTesting
  static final String SUBMIT_INCREMENT_COUNTER_TX_PATH = "/submit-increment-counter";
  @VisibleForTesting
  static final String SUBMIT_INVALID_TX_PATH = "/submit-invalid";
  @VisibleForTesting
  static final String SUBMIT_INVALID_THROWING_TX_PATH = "/submit-invalid-throwing";
  @VisibleForTesting
  static final String SUBMIT_VALID_THROWING_TX_PATH = "/submit-valid-throwing";
  @VisibleForTesting
  static final String SUBMIT_UNKNOWN_TX_PATH = "/submit-unknown";
  private static final String COUNTER_ID_PARAM = "counterId";
  private static final String GET_COUNTER_PATH = "/counter/:" + COUNTER_ID_PARAM;

  private static final Logger logger = LogManager.getLogger(ApiController.class);

  private final QaService service;

  @Inject
  ApiController(QaService service) {
    this.service = service;
  }

  void mountApi(Router router) {
    // Mount the body handler to process bodies of some POST queries, and the handler of failures.
    router.route()
        .handler(BodyHandler.create())
        .failureHandler(this::failureHandler);

    // Mount the handlers of each request
    ImmutableMap<String, Handler<RoutingContext>> handlers =
        ImmutableMap.<String, Handler<RoutingContext>>builder()
            .put(SUBMIT_CREATE_COUNTER_TX_PATH, this::submitCreateCounter)
            .put(SUBMIT_INCREMENT_COUNTER_TX_PATH, this::submitIncrementCounter)
            .put(SUBMIT_INVALID_TX_PATH, this::submitInvalidTx)
            .put(SUBMIT_INVALID_THROWING_TX_PATH, this::submitInvalidThrowingTx)
            .put(SUBMIT_VALID_THROWING_TX_PATH, this::submitValidThrowingTx)
            .put(SUBMIT_UNKNOWN_TX_PATH, this::submitUnknownTx)
            .put(GET_COUNTER_PATH, this::getCounter)
            .build();

    handlers.forEach((path, handler) ->
        router.route(path).handler(handler)
    );
  }

  private void submitCreateCounter(RoutingContext rc) {
    MultiMap parameters = rc.request().params();
    String name = getRequiredParameter(parameters, "name");

    HashCode txHash = service.submitCreateCounter(name);
    replyTxSubmitted(rc, txHash);
  }

  private void submitIncrementCounter(RoutingContext rc) {
    MultiMap parameters = rc.request().params();
    long seed = getRequiredParameter(parameters, "seed", Long::parseLong);
    HashCode counterId = getRequiredParameter(parameters, COUNTER_ID_PARAM, HashCode::fromString);

    HashCode txHash = service.submitIncrementCounter(seed, counterId);
    replyTxSubmitted(rc, txHash);
  }

  private void submitInvalidTx(RoutingContext rc) {
    HashCode txHash = service.submitInvalidTx();
    replyTxSubmitted(rc, txHash);
  }

  private void submitInvalidThrowingTx(RoutingContext rc) {
    HashCode txHash = service.submitInvalidThrowingTx();
    replyTxSubmitted(rc, txHash);
  }

  private void submitValidThrowingTx(RoutingContext rc) {
    MultiMap parameters = rc.request().params();
    long seed = getRequiredParameter(parameters, "seed", Long::parseLong);

    HashCode txHash = service.submitValidThrowingTx(seed);
    replyTxSubmitted(rc, txHash);
  }

  private void submitUnknownTx(RoutingContext rc) {
    HashCode txHash = service.submitUnknownTx();
    replyTxSubmitted(rc, txHash);
  }

  private void getCounter(RoutingContext rc) {
    HashCode counterId = getRequiredParameter(rc.request(), COUNTER_ID_PARAM, HashCode::fromString);

    Optional<Counter> counter = service.getValue(counterId);

    if (counter.isPresent()) {
      Gson gson = QaTransactionGson.instance();
      rc.response()
          .putHeader("Content-Type", "application/json")
          .end(gson.toJson(counter.get()));
    } else {
      rc.response()
          .setStatusCode(HTTP_NOT_FOUND)
          .end();
    }
  }

  private static String getRequiredParameter(MultiMap parameters, String key) {
    return getRequiredParameter(parameters, key, String::toString);
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

  private void replyTxSubmitted(RoutingContext rc, HashCode transactionHash) {
    rc.response()
        .setStatusCode(HTTP_CREATED)
        .putHeader("Content-Type", "text/plain")
        .putHeader("Location", transactionLocationPath(transactionHash))
        .end(String.valueOf(transactionHash));
  }

  private String transactionLocationPath(HashCode txHash) {
    return "/api/explorer/v1/transactions/" + txHash;
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
    // All IllegalArgumentExceptions (including NumberFormatException) are considered
    // to be caused by a bad request.
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
}

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

import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static com.exonum.binding.qaservice.ApiController.QaPaths.COUNTER_ID_PARAM;
import static com.exonum.binding.qaservice.ApiController.QaPaths.GET_ACTUAL_CONFIGURATION_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.GET_COUNTER_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.SUBMIT_CREATE_COUNTER_TX_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.SUBMIT_INCREMENT_COUNTER_TX_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.SUBMIT_UNKNOWN_TX_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.SUBMIT_VALID_ERROR_TX_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.SUBMIT_VALID_THROWING_TX_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.TIME_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.VALIDATORS_TIMES_PATH;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.LOCATION;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import com.exonum.binding.common.configuration.StoredConfiguration;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class ApiController {

  private static final Logger logger = LogManager.getLogger(ApiController.class);

  private final QaService service;

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
            .put(SUBMIT_VALID_THROWING_TX_PATH, this::submitValidThrowingTx)
            .put(SUBMIT_VALID_ERROR_TX_PATH, this::submitValidErrorTx)
            .put(SUBMIT_UNKNOWN_TX_PATH, this::submitUnknownTx)
            .put(GET_COUNTER_PATH, this::getCounter)
            .put(GET_ACTUAL_CONFIGURATION_PATH, this::getActualConfiguration)
            .put(TIME_PATH, this::getTime)
            .put(VALIDATORS_TIMES_PATH, this::getValidatorsTimes)
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

  private void submitValidThrowingTx(RoutingContext rc) {
    MultiMap parameters = rc.request().params();
    long seed = getRequiredParameter(parameters, "seed", Long::parseLong);

    HashCode txHash = service.submitValidThrowingTx(seed);
    replyTxSubmitted(rc, txHash);
  }

  private void submitValidErrorTx(RoutingContext rc) {
    MultiMap parameters = rc.request().params();
    long seed = getRequiredParameter(parameters, "seed", Long::parseLong);
    byte errorCode = getRequiredParameter(parameters, "errorCode", Byte::parseByte);
    String description = parameters.get("errorDescription");

    HashCode txHash = service.submitValidErrorTx(seed, errorCode, description);
    replyTxSubmitted(rc, txHash);
  }

  private void submitUnknownTx(RoutingContext rc) {
    HashCode txHash = service.submitUnknownTx();
    replyTxSubmitted(rc, txHash);
  }

  private void getCounter(RoutingContext rc) {
    HashCode counterId = getRequiredParameter(rc.request(), COUNTER_ID_PARAM, HashCode::fromString);

    Optional<Counter> counter = service.getValue(counterId);

    respondWithJson(rc, counter);
  }

  private void getActualConfiguration(RoutingContext rc) {
    StoredConfiguration configuration = service.getActualConfiguration();
    respondWithJson(rc, configuration);
  }

  private void getTime(RoutingContext rc) {
    Optional<TimeDto> time = service.getTime().map(TimeDto::new);
    respondWithJson(rc, time);
  }

  private void getValidatorsTimes(RoutingContext rc) {
    Map<PublicKey, ZonedDateTime> validatorsTimes = service.getValidatorsTimes();
    respondWithJson(rc, validatorsTimes);
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
        .putHeader(CONTENT_TYPE, "text/plain")
        .putHeader(LOCATION, transactionLocationPath(transactionHash))
        .end(String.valueOf(transactionHash));
  }

  private String transactionLocationPath(HashCode txHash) {
    return "/api/explorer/v1/transactions/" + txHash;
  }

  private void failureHandler(RoutingContext rc) {
    logger.info("An error whilst processing request {}", rc.normalisedPath());

    Throwable requestFailure = rc.failure();
    if (requestFailure != null) {
      HttpServerResponse response = rc.response();
      if (isBadRequest(requestFailure)) {
        logger.info("Request error:", requestFailure);
        response.setStatusCode(HTTP_BAD_REQUEST);
      } else {
        logger.error("Internal error", requestFailure);
        response.setStatusCode(HTTP_INTERNAL_ERROR);
      }
      String description = Strings.nullToEmpty(requestFailure.getMessage());
      response.putHeader(CONTENT_TYPE, "text/plain")
          .end(description);
    } else {
      int failureStatusCode = rc.statusCode();
      rc.response()
          .setStatusCode(failureStatusCode)
          .end();
    }
  }

  /**
   * Returns true if the passed throwable corresponds to a bad request; false — otherwise.
   */
  private boolean isBadRequest(Throwable requestFailure) {
    // All IllegalArgumentExceptions (including NumberFormatException) and IndexOutOfBoundsException
    // are considered to be caused by a bad request. Other Throwables are considered internal
    // errors.
    return (requestFailure instanceof IllegalArgumentException
        || requestFailure instanceof IndexOutOfBoundsException);
  }

  private <T> void respondWithJson(RoutingContext rc, Optional<T> responseBody) {
    if (responseBody.isPresent()) {
      respondWithJson(rc, responseBody.get());
    } else {
      respondNotFound(rc);
    }
  }

  private void respondWithJson(RoutingContext rc, Object responseBody) {
    rc.response()
        .putHeader(CONTENT_TYPE, "application/json")
        .end(json().toJson(responseBody));
  }

  private void respondNotFound(RoutingContext rc) {
    rc.response()
        .setStatusCode(HTTP_NOT_FOUND)
        .end();
  }

  static class QaPaths {
    @VisibleForTesting
    static final String SUBMIT_CREATE_COUNTER_TX_PATH = "/submit-create-counter";
    @VisibleForTesting
    static final String SUBMIT_INCREMENT_COUNTER_TX_PATH = "/submit-increment-counter";
    @VisibleForTesting
    static final String SUBMIT_VALID_THROWING_TX_PATH = "/submit-valid-throwing";
    @VisibleForTesting
    static final String SUBMIT_VALID_ERROR_TX_PATH = "/submit-valid-error";
    @VisibleForTesting
    static final String SUBMIT_UNKNOWN_TX_PATH = "/submit-unknown";
    static final String COUNTER_ID_PARAM = "counterId";
    static final String GET_COUNTER_PATH = "/counter/:" + COUNTER_ID_PARAM;
    @VisibleForTesting
    static final String GET_ACTUAL_CONFIGURATION_PATH = "/actualConfiguration";
    @VisibleForTesting
    static final String TIME_PATH = "/time";
    @VisibleForTesting
    static final String VALIDATORS_TIMES_PATH = TIME_PATH + "/validators";
  }

}

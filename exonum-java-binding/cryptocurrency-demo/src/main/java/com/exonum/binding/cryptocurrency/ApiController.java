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
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import com.exonum.binding.common.crypto.PublicKey;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Controller for submitting transactions.
 */
final class ApiController {

  private static final Logger logger = LogManager.getLogger(ApiController.class);

  private static final String WALLET_ID_PARAM = "walletId";
  private static final String GET_WALLET_PATH = "/wallet/:" + WALLET_ID_PARAM;
  private static final String GET_WALLET_HISTORY_PATH = "/wallet/:" + WALLET_ID_PARAM + "/history";

  private final CryptocurrencyService service;

  ApiController(CryptocurrencyService service) {
    this.service = service;
  }

  void mountApi(Router router) {
    router.route().failureHandler(this::failureHandler);

    ImmutableMap<String, Handler<RoutingContext>> handlers = ImmutableMap.of(
        GET_WALLET_PATH, this::getWallet,
        GET_WALLET_HISTORY_PATH, this::getWalletHistory);

    handlers.forEach((path, handler) ->
        router.route(path).handler(handler)
    );
  }

  private void getWallet(RoutingContext rc) {
    PublicKey walletId =
        getRequiredParameter(rc.request(), WALLET_ID_PARAM, PublicKey::fromHexString);

    Optional<Wallet> wallet = service.getWallet(walletId);

    if (wallet.isPresent()) {
      rc.response()
          .putHeader(CONTENT_TYPE, "application/json")
          .end(json().toJson(wallet.get()));
    } else {
      rc.response()
          .setStatusCode(HTTP_NOT_FOUND)
          .end();
    }
  }

  private void getWalletHistory(RoutingContext rc) {
    PublicKey walletId =
        getRequiredParameter(rc.request(), WALLET_ID_PARAM, PublicKey::fromHexString);
    List<HistoryEntity> walletHistory = service.getWalletHistory(walletId);

    rc.response()
        .putHeader(CONTENT_TYPE, "application/json")
        .end(json().toJson(walletHistory));
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

    // This throwable must correspond to an internal server error.
    return Optional.empty();
  }
}

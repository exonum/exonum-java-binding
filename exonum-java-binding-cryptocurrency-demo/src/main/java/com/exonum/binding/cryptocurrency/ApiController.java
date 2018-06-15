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

import com.exonum.binding.cryptocurrency.transactions.BaseTx;
import com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransaction;
import com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionGson;
import com.exonum.binding.hash.HashCode;
import com.exonum.binding.messages.InternalServerError;
import com.exonum.binding.messages.InvalidTransactionException;
import com.exonum.binding.messages.Transaction;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Controller for submitting transactions. */
final class ApiController {

  @VisibleForTesting static final String SUBMIT_TRANSACTION_PATH = "/submit-transaction";
  private static final Logger log = LogManager.getLogger(ApiController.class);
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
    String submitTxPath = SUBMIT_TRANSACTION_PATH;
    router.route(submitTxPath).handler(BodyHandler.create());
    router.route(submitTxPath).handler(this::submitTransaction);
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

    try {
      // Submit an executable transaction to the network
      HashCode txHash = service.submitTransaction(tx);
      rc.response().end(String.valueOf(txHash));
    } catch (InvalidTransactionException e) {
      rc.response()
          .setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST)
          .setStatusMessage("Bad Request: transaction is not valid")
          .end();
    } catch (InternalServerError e) {
      log.error("Internal error occurred whilst submitting tx (id={}, tx={}):", txId, tx, e);
      rc.response().setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR).end();
    }
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

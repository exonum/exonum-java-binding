package com.exonum.binding.qaservice;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.messages.InternalServerError;
import com.exonum.binding.messages.InvalidTransactionException;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.qaservice.transactions.AnyTransaction;
import com.exonum.binding.qaservice.transactions.QaTransaction;
import com.exonum.binding.qaservice.transactions.QaTransactionGson;
import com.exonum.binding.service.Node;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.util.Types;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * With JSON and stuff.
 */
class ApiController2 {

  private static final Logger logger = LogManager.getLogger(ApiController2.class);

  @VisibleForTesting
  static final String SUBMIT_TRANSACTION_PATH = "/submit-transaction";

  private static final Map<Short, Type> TX_MESSAGE_TYPES = Arrays.stream(QaTransaction.values())
          .collect(Collectors.toMap(
              QaTransaction::id,
              tx -> Types.newParameterizedType(AnyTransaction.class, tx.transactionClass())
          ));

  private final Node node;

  @Inject
  ApiController2(Node node) {
    this.node = node;
  }

  void mountApi(Router router) {
    String submitTxPath = SUBMIT_TRANSACTION_PATH;
    router.route(submitTxPath).handler(BodyHandler.create());
    router.route(submitTxPath).handler(this::submitTransaction);
  }

  void submitTransaction(RoutingContext rc) {
    Gson gson = QaTransactionGson.instance();
    // Extract the transaction type from the body
    String body = rc.getBodyAsString();
    AnyTransaction rawTxMessage = gson.fromJson(body, AnyTransaction.class);
    short serviceId = rawTxMessage.service_id;
    if (serviceId != QaService.ID) {
      rc.response()
          .setStatusCode(HTTP_BAD_REQUEST)
          .end(String.format("Unknown service id (%d), must be (%d)", rawTxMessage.service_id,
              QaService.ID));
      return;
    }

    // Convert JSON -> Transaction (or directly Map<String, Object> -> Transaction?)
    short txId = rawTxMessage.message_id;
    Optional<Transaction> maybeTx = txFromMessage(rc, txId, body);
    if (!maybeTx.isPresent()) {
      return;
    }
    Transaction tx = maybeTx.get();

    try {
      // Submit an executable transaction to the network
      node.submitTransaction(tx);
      // Send the OK response with the hash of submitted transaction
      HashCode txHash = tx.hash();
      rc.response()
          .end(String.valueOf(txHash));
    } catch (InvalidTransactionException e) {
      rc.response()
          .setStatusCode(HTTP_BAD_REQUEST)
          .setStatusMessage("Bad Request: transaction is not valid")
          .end();
    } catch (InternalServerError e) {
      logger.error(e);
      rc.response()
          .setStatusCode(HTTP_INTERNAL_ERROR)
          .end();
    }
  }

  private Optional<Transaction> txFromMessage(RoutingContext rc, short txId, String txMessageJson) {
    if (!TX_MESSAGE_TYPES.containsKey(txId)) {
      rc.response()
          .setStatusCode(HTTP_BAD_REQUEST)
          .end(String.format("Unknown transaction id (%d)", txId));
      return Optional.empty();
    }
    Gson gson = QaTransactionGson.instance();
    Type txMessageType = TX_MESSAGE_TYPES.get(txId);
    try {
      AnyTransaction<? extends Transaction> txMessage = gson.fromJson(txMessageJson, txMessageType);
      return Optional.of(txMessage.body);
    } catch (JsonSyntaxException e) {
      throw new AssertionError("JSON must have been already validated");
    } catch (JsonParseException e) {
      rc.response()
          .setStatusCode(HTTP_BAD_REQUEST)
          .end(String.format("Cannot convert transaction (%d) into an instance "
              + "of the corresponding class (%s)", txId, txMessageType));
      return Optional.empty();
    }
  }
}

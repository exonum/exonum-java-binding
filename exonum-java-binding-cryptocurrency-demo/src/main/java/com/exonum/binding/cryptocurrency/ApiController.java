package com.exonum.binding.cryptocurrency;

import com.exonum.binding.cryptocurrency.transactions.AnyTransaction;
import com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransaction;
import com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionGson;
import com.exonum.binding.hash.HashCode;
import com.exonum.binding.messages.InternalServerError;
import com.exonum.binding.messages.InvalidTransactionException;
import com.exonum.binding.messages.Transaction;
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
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * With JSON and stuff.
 */
class ApiController {

    private static final Logger LOG = LogManager.getLogger(ApiController.class);

    @VisibleForTesting
    static final String SUBMIT_TRANSACTION_PATH = "/submit-transaction";

    private static final Map<Short, Type> TX_MESSAGE_TYPES = Arrays.stream(CryptocurrencyTransaction.values())
            .collect(Collectors.toMap(
                    CryptocurrencyTransaction::getId,
                    tx -> Types.newParameterizedType(tx.transactionClass(), AnyTransaction.class)
            ));

    private final Node node;

    @Inject
    ApiController(Node node) {
        this.node = node;
    }

    void mountApi(Router router) {
        String submitTxPath = SUBMIT_TRANSACTION_PATH;
        router.route(submitTxPath).handler(BodyHandler.create());
        router.route(submitTxPath).handler(this::submitTransaction);
    }

    void submitTransaction(RoutingContext rc) {
        Gson gson = CryptocurrencyTransactionGson.instance();
        // Extract the transaction type from the body
        String body = rc.getBodyAsString();
        AnyTransaction rawTxMessage = gson.fromJson(body, AnyTransaction.class);
        short serviceId = rawTxMessage.getService_id();
        if (serviceId != CryptocurrencyService.ID) {
            rc.response()
                    .setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST)
                    .end(String.format("Unknown service getId (%d), must be (%d)", rawTxMessage.getService_id(),
                            CryptocurrencyService.ID));
            return;
        }

        // Convert JSON -> Transaction (or directly Map<String, Object> -> Transaction?)
        short txId = rawTxMessage.getMessage_id();
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
                    .setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST)
                    .setStatusMessage("Bad Request: transaction is not valid")
                    .end();
        } catch (InternalServerError e) {
            LOG.log(Level.ERROR, e);
            rc.response()
                    .setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
                    .end();
        }
    }

    private Optional<Transaction> txFromMessage(RoutingContext rc, short txId, String txMessageJson) {
        if (!TX_MESSAGE_TYPES.containsKey(txId)) {
            rc.response()
                    .setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST)
                    .end(String.format("Unknown transaction getId (%d)", txId));
            return Optional.empty();
        }
        Gson gson = CryptocurrencyTransactionGson.instance();
        Type txMessageType = TX_MESSAGE_TYPES.get(txId);
        try {
//            AnyTransaction<? extends Transaction> txMessage = gson.fromJson(txMessageJson, txMessageType);
            AnyTransaction<? extends Transaction> txMessage = gson.fromJson(txMessageJson, txMessageType);
            return Optional.of(txMessage.getBody());
        } catch (JsonSyntaxException e) {
            throw new AssertionError("JSON must have been already validated");
        } catch (JsonParseException e) {
            rc.response()
                    .setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST)
                    .end(String.format("Cannot convert transaction (%d) into an instance "
                            + "of the corresponding class (%s)", txId, txMessageType));
            return Optional.empty();
        }
    }
}

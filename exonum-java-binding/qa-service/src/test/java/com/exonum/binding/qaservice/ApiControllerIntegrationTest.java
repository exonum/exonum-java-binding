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

import static com.exonum.binding.common.hash.Hashing.sha256;
import static com.exonum.binding.qaservice.ApiController.QaPaths.BLOCKCHAIN_BLOCKS_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.BLOCKCHAIN_BLOCK_HASHES_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.BLOCKCHAIN_BLOCK_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.BLOCKCHAIN_BLOCK_TRANSACTIONS_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.BLOCKCHAIN_HEIGHT_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.BLOCKCHAIN_LAST_BLOCK_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.BLOCKCHAIN_TRANSACTION_LOCATIONS_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.BLOCKCHAIN_TRANSACTION_LOCATION_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.BLOCKCHAIN_TRANSACTION_MESSAGES_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.BLOCKCHAIN_TRANSACTION_RESULTS_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.BLOCKCHAIN_TRANSACTION_RESULT_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.BLOCK_HEIGHT_PARAM;
import static com.exonum.binding.qaservice.ApiController.QaPaths.BLOCK_ID_PARAM;
import static com.exonum.binding.qaservice.ApiController.QaPaths.GET_ACTUAL_CONFIGURATION_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.MESSAGE_HASH_PARAM;
import static com.exonum.binding.qaservice.ApiController.QaPaths.SUBMIT_CREATE_COUNTER_TX_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.SUBMIT_INCREMENT_COUNTER_TX_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.SUBMIT_UNKNOWN_TX_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.SUBMIT_VALID_ERROR_TX_PATH;
import static com.exonum.binding.qaservice.ApiController.QaPaths.SUBMIT_VALID_THROWING_TX_PATH;
import static com.exonum.binding.qaservice.ApiController.hexEncodeTransactionMessages;
import static com.google.common.base.Preconditions.checkArgument;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.blockchain.Block;
import com.exonum.binding.blockchain.TransactionLocation;
import com.exonum.binding.blockchain.TransactionResult;
import com.exonum.binding.blockchain.serialization.BlockAdapterFactory;
import com.exonum.binding.blockchain.serialization.TransactionLocationAdapterFactory;
import com.exonum.binding.blockchain.serialization.TransactionResultAdapterFactory;
import com.exonum.binding.common.configuration.ConsensusConfiguration;
import com.exonum.binding.common.configuration.StoredConfiguration;
import com.exonum.binding.common.configuration.ValidatorKey;
import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.HashFunction;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.common.serialization.json.JsonSerializer;
import com.exonum.binding.service.InternalServerError;
import com.exonum.binding.test.Bytes;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@SuppressWarnings("WeakerAccess")
class ApiControllerIntegrationTest {

  private static final String HOST = "0.0.0.0";

  private static final HashCode EXPECTED_TX_HASH = sha256().hashInt(1);

  private static final String HASH_STRING = "ab";
  private static final HashCode HASH_1 = HashCode.fromInt(0x00);
  private static final HashCode HASH_2 = HashCode.fromInt(0x01);

  private static final Gson JSON_SERIALIZER = JsonSerializer.builder()
      .registerTypeAdapterFactory(BlockAdapterFactory.create())
      .registerTypeAdapterFactory(TransactionLocationAdapterFactory.create())
      .registerTypeAdapterFactory(TransactionResultAdapterFactory.create())
      .create();

  @Mock
  QaService qaService;

  HttpServer httpServer;

  WebClient webClient;

  volatile int port = -1;

  @BeforeEach
  void setup(Vertx vertx, VertxTestContext context) {
    httpServer = vertx.createHttpServer();
    webClient = WebClient.create(vertx);

    Router router = Router.router(vertx);
    new ApiController(qaService).mountApi(router);

    httpServer.requestHandler(router::accept)
        .listen(0, context.succeeding(result -> {

          // Set the actual server port.
          port = result.actualPort();

          context.completeNow();
        }));
  }

  @AfterEach
  void tearDown(VertxTestContext context) {
    webClient.close();
    httpServer.close(context.succeeding(r -> context.completeNow()));
  }

  @Test
  void submitCreateCounter(VertxTestContext context) {
    String counterName = "counter 1";
    MultiMap params = multiMap("name", counterName);

    when(qaService.submitCreateCounter(eq(counterName)))
        .thenReturn(EXPECTED_TX_HASH);

    post(SUBMIT_CREATE_COUNTER_TX_PATH)
        .sendForm(params, checkCreatedTransaction(context, EXPECTED_TX_HASH));
  }

  @Test
  void submitCreateCounter_NoParameter(VertxTestContext context) {
    post(SUBMIT_CREATE_COUNTER_TX_PATH)
        .sendForm(MultiMap.caseInsensitiveMultiMap(), context.succeeding(response -> {
          context.verify(() -> {
            assertThat(response.statusCode()).isEqualTo(HTTP_BAD_REQUEST);

            assertThat(response.bodyAsString())
                .contains("No required key (name) in request parameters:");

            context.completeNow();
          });
        }));
  }

  @Test
  void submitCreateCounter_IllegalArgumentSomewhere(VertxTestContext context) {
    String counterName = "counter 1";
    MultiMap params = multiMap("name", counterName);

    Throwable error = mock(IllegalArgumentException.class);
    when(qaService.submitCreateCounter(counterName))
        .thenThrow(error);

    post(SUBMIT_CREATE_COUNTER_TX_PATH)
        .sendForm(params, context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode()).isEqualTo(HTTP_BAD_REQUEST);
          context.completeNow();
        })));
  }

  @Test
  void submitCreateCounter_InternalServerError(VertxTestContext context) {
    String counterName = "counter 1";
    MultiMap params = multiMap("name", counterName);

    Throwable error = wrappingChecked(InternalServerError.class);
    when(qaService.submitCreateCounter(counterName))
        .thenThrow(error);

    post(SUBMIT_CREATE_COUNTER_TX_PATH)
        .sendForm(params, context.succeeding(response -> {
          context.verify(() -> {
            assertThat(response.statusCode()).isEqualTo(HTTP_INTERNAL_ERROR);
            context.completeNow();
          });
        }));
  }

  @Test
  void submitIncrementCounter(VertxTestContext context) {
    long seed = 1L;
    HashCode counterId = HASH_1;
    MultiMap params = multiMap("seed", Long.toString(seed),
        "counterId", String.valueOf(counterId));

    when(qaService.submitIncrementCounter(eq(seed), eq(counterId)))
        .thenReturn(EXPECTED_TX_HASH);

    post(SUBMIT_INCREMENT_COUNTER_TX_PATH)
        .sendForm(params, checkCreatedTransaction(context, EXPECTED_TX_HASH));
  }

  @Test
  void submitValidThrowing(VertxTestContext context) {
    long seed = 10L;

    when(qaService.submitValidThrowingTx(seed))
        .thenReturn(EXPECTED_TX_HASH);

    MultiMap form = multiMap("seed", Long.toString(seed));

    post(SUBMIT_VALID_THROWING_TX_PATH)
        .sendForm(form, checkCreatedTransaction(context, EXPECTED_TX_HASH));
  }

  @Test
  void submitValidError(VertxTestContext context) {
    long seed = 1L;
    byte errorCode = 2;
    String description = "Boom";
    MultiMap params = multiMap("seed", Long.toString(seed),
        "errorCode", Byte.toString(errorCode),
        "errorDescription", description);

    when(qaService.submitValidErrorTx(eq(seed), eq(errorCode), eq(description)))
        .thenReturn(EXPECTED_TX_HASH);

    post(SUBMIT_VALID_ERROR_TX_PATH)
        .sendForm(params, checkCreatedTransaction(context, EXPECTED_TX_HASH));
  }

  @Test
  void submitValidErrorNoDescription(VertxTestContext context) {
    long seed = 1L;
    byte errorCode = 2;
    MultiMap params = multiMap("seed", Long.toString(seed),
        "errorCode", Byte.toString(errorCode));

    when(qaService.submitValidErrorTx(eq(seed), eq(errorCode), isNull()))
        .thenReturn(EXPECTED_TX_HASH);

    post(SUBMIT_VALID_ERROR_TX_PATH)
        .sendForm(params, checkCreatedTransaction(context, EXPECTED_TX_HASH));
  }

  @Test
  void submitUnknown(VertxTestContext context) {
    when(qaService.submitUnknownTx())
        .thenReturn(EXPECTED_TX_HASH);

    post(SUBMIT_UNKNOWN_TX_PATH)
        .send(checkCreatedTransaction(context, EXPECTED_TX_HASH));
  }

  @Test
  void getCounter(VertxTestContext context) {
    HashCode id = HASH_1;
    String name = "counter";
    long value = 10L;
    Counter counter = new Counter(name, value);
    when(qaService.getValue(eq(id)))
        .thenReturn(Optional.of(counter));

    String getCounterUri = getCounterUri(id);
    get(getCounterUri)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          Counter actualCounter = JSON_SERIALIZER.fromJson(body, Counter.class);
          assertThat(actualCounter).isEqualTo(counter);

          context.completeNow();
        })));
  }

  @Test
  void getCounter_NoCounter(VertxTestContext context) {
    HashCode id = HASH_1;
    when(qaService.getValue(id))
        .thenReturn(Optional.empty());

    String getCounterUri = getCounterUri(id);
    get(getCounterUri)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode()).isEqualTo(HTTP_NOT_FOUND);

          context.completeNow();
        })));
  }

  @Test
  void getCounter_InvalidIdFormat(VertxTestContext context) {
    String hash = "Invalid hexadecimal hash";
    String getCounterUri = getCounterUri(hash);

    get(getCounterUri)
        .send(context.succeeding(response -> context.verify(() -> {
          assertAll(
              () -> assertThat(response.statusCode()).isEqualTo(HTTP_BAD_REQUEST),
              () -> assertThat(response.bodyAsString())
                  .startsWith("Failed to convert parameter (counterId):"));
          context.completeNow();
        })));
  }

  @Test
  void multiMapTest() {
    MultiMap m = multiMap("k1", "v1",
        "k2", "v2",
        "k3", "v3");

    assertThat(m.get("k1")).isEqualTo("v1");
    assertThat(m.get("k2")).isEqualTo("v2");
    assertThat(m.get("k3")).isEqualTo("v3");
  }

  @Test
  void getHeight(VertxTestContext context) {
    Height height = new Height(10L);

    when(qaService.getHeight()).thenReturn(height);

    get(BLOCKCHAIN_HEIGHT_PATH)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          Height actualHeight = JSON_SERIALIZER.fromJson(body, Height.class);
          assertThat(actualHeight).isEqualTo(height);

          context.completeNow();
        })));
  }

  @Test
  void getHeight_throwsException(VertxTestContext context) {
    when(qaService.getHeight()).thenThrow(new RuntimeException());

    get(BLOCKCHAIN_HEIGHT_PATH)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_BAD_REQUEST);

          context.completeNow();
        })));
  }

  @Test
  void getBlockHashes(VertxTestContext context) {
    List<HashCode> blockHashes = Arrays.asList(HASH_1, HASH_2);

    when(qaService.getBlockHashes()).thenReturn(blockHashes);

    get(BLOCKCHAIN_BLOCK_HASHES_PATH)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          Object actualHashes = JSON_SERIALIZER
              .fromJson(body, new TypeToken<List<HashCode>>() {
              }.getType());
          assertThat(actualHashes).isEqualTo(blockHashes);

          context.completeNow();
        })));
  }

  @Test
  void getBlockTransactionsByHeight(VertxTestContext context) {
    List<HashCode> transactionHashes = Arrays
        .asList(HASH_1, HASH_2);

    when(qaService.getBlockTransactions(anyLong())).thenReturn(transactionHashes);

    get(BLOCKCHAIN_BLOCK_TRANSACTIONS_PATH).setQueryParam(BLOCK_HEIGHT_PARAM, "123")
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          Object actualHashes = JSON_SERIALIZER
              .fromJson(body, new TypeToken<List<HashCode>>() {
              }.getType());
          assertThat(actualHashes).isEqualTo(transactionHashes);

          context.completeNow();
        })));
  }

  @Test
  void getBlockTransactionsByBlockId(VertxTestContext context) {
    List<HashCode> transactionHashes = Arrays
        .asList(HASH_1, HASH_2);

    HashCode blockId = HashCode.fromString(HASH_STRING);
    when(qaService.getBlockTransactions(blockId)).thenReturn(transactionHashes);

    get(BLOCKCHAIN_BLOCK_TRANSACTIONS_PATH).setQueryParam(BLOCK_ID_PARAM, HASH_STRING)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          Object actualHashes = JSON_SERIALIZER
              .fromJson(body, new TypeToken<List<HashCode>>() {
              }.getType());
          assertThat(actualHashes).isEqualTo(transactionHashes);

          context.completeNow();
        })));
  }

  @Test
  void getBlockTransactions_InvalidRequest(VertxTestContext context) {
    get(BLOCKCHAIN_BLOCK_TRANSACTIONS_PATH)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_BAD_REQUEST);

          context.completeNow();
        })));
  }

  @Test
  void getTxMessages(VertxTestContext context) {
    Map<HashCode, TransactionMessage> transactionMessages = Maps.uniqueIndex(
        Arrays.asList(
            transactionMessageSource("first transaction"),
            transactionMessageSource("second transaction")),
        TransactionMessage::hash);

    when(qaService.getTxMessages()).thenReturn(transactionMessages);

    Map<HashCode, String> transactionMessagesEncoded =
        hexEncodeTransactionMessages(transactionMessages);

    get(BLOCKCHAIN_TRANSACTION_MESSAGES_PATH)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          Object actualMessages = JSON_SERIALIZER
              .fromJson(body, new TypeToken<Map<HashCode, String>>() {
              }.getType());
          assertThat(actualMessages).isEqualTo(transactionMessagesEncoded);

          context.completeNow();
        })));
  }

  @Test
  void getTxResults(VertxTestContext context) {
    Map<HashCode, TransactionResult> txResults = ImmutableMap.of(
        HASH_1, TransactionResult.successful(),
        HASH_2, TransactionResult.error(1, "Error description"));

    when(qaService.getTxResults()).thenReturn(txResults);

    get(BLOCKCHAIN_TRANSACTION_RESULTS_PATH)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          Object actualTxResults = JSON_SERIALIZER
              .fromJson(body, new TypeToken<Map<HashCode, TransactionResult>>() {
              }.getType());
          assertThat(actualTxResults).isEqualTo(txResults);

          context.completeNow();
        })));
  }

  @Test
  void getTxResult(VertxTestContext context) {
    TransactionResult transactionResult = TransactionResult.successful();

    HashCode messageHash = HashCode.fromString(HASH_STRING);
    when(qaService.getTxResult(messageHash)).thenReturn(Optional.of(transactionResult));

    get(BLOCKCHAIN_TRANSACTION_RESULT_PATH.replace(":" + MESSAGE_HASH_PARAM, HASH_STRING))
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          Object actualResult = JSON_SERIALIZER
              .fromJson(body, new TypeToken<TransactionResult>() {
              }.getType());
          assertThat(actualResult).isEqualTo(transactionResult);

          context.completeNow();
        })));
  }

  @Test
  void getNonexistentTxResult(VertxTestContext context) {
    HashCode messageHash = HashCode.fromString(HASH_STRING);
    when(qaService.getTxResult(messageHash)).thenReturn(Optional.empty());

    get(BLOCKCHAIN_TRANSACTION_RESULT_PATH.replace(":" + MESSAGE_HASH_PARAM, HASH_STRING))
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_NOT_FOUND);

          context.completeNow();
        })));
  }

  @Test
  void getTxLocations(VertxTestContext context) {
    Map<HashCode, TransactionLocation> txLocations = ImmutableMap.of(
        HASH_1, TransactionLocation.valueOf(1L, 1L),
        HASH_2, TransactionLocation.valueOf(1L, 2L));

    when(qaService.getTxLocations()).thenReturn(txLocations);

    get(BLOCKCHAIN_TRANSACTION_LOCATIONS_PATH)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          Object actualTxLocations = JSON_SERIALIZER
              .fromJson(body, new TypeToken<Map<HashCode, TransactionLocation>>() {
              }.getType());
          assertThat(actualTxLocations).isEqualTo(txLocations);

          context.completeNow();
        })));
  }

  @Test
  void getTxLocation(VertxTestContext context) {
    TransactionLocation transactionLocation = TransactionLocation.valueOf(1L, 1L);

    HashCode messageHash = HashCode.fromString(HASH_STRING);
    when(qaService.getTxLocation(messageHash)).thenReturn(Optional.of(transactionLocation));

    get(BLOCKCHAIN_TRANSACTION_LOCATION_PATH.replace(":" + MESSAGE_HASH_PARAM, HASH_STRING))
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          Object actualLocation = JSON_SERIALIZER
              .fromJson(body, new TypeToken<TransactionLocation>() {
              }.getType());
          assertThat(actualLocation).isEqualTo(transactionLocation);

          context.completeNow();
        })));
  }

  @Test
  void getNonexistentTxLocation(VertxTestContext context) {
    HashCode messageHash = HashCode.fromString(HASH_STRING);
    when(qaService.getTxLocation(messageHash)).thenReturn(Optional.empty());

    get(BLOCKCHAIN_TRANSACTION_LOCATION_PATH.replace(":" + MESSAGE_HASH_PARAM, HASH_STRING))
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_NOT_FOUND);

          context.completeNow();
        })));
  }

  @Test
  void getBlocks(VertxTestContext context) {
    Map<HashCode, Block> blocks = Maps.uniqueIndex(
        Arrays.asList(createBlock(1L), createBlock(2L)),
        Block::getBlockHash);

    when(qaService.getBlocks()).thenReturn(blocks);

    get(BLOCKCHAIN_BLOCKS_PATH)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          Object actualBlocks = JSON_SERIALIZER
              .fromJson(body, new TypeToken<Map<HashCode, Block>>() {
              }.getType());
          assertThat(actualBlocks).isEqualTo(blocks);

          context.completeNow();
        })));
  }

  @Test
  void getBlockByHeight(VertxTestContext context) {
    Block block = createBlock(1L);

    long blockHeight = block.getHeight();
    when(qaService.getBlockByHeight(blockHeight)).thenReturn(block);

    get(BLOCKCHAIN_BLOCK_PATH).setQueryParam(BLOCK_HEIGHT_PARAM, String.valueOf(blockHeight))
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          Object actualBlock = JSON_SERIALIZER
              .fromJson(body, new TypeToken<Block>() {
              }.getType());
          assertThat(actualBlock).isEqualTo(block);

          context.completeNow();
        })));
  }

  @Test
  void getNonexistentBlockByHeight(VertxTestContext context) {
    long blockHeight = 1L;

    when(qaService.getBlockByHeight(blockHeight)).thenThrow(new IndexOutOfBoundsException());

    get(BLOCKCHAIN_BLOCK_PATH).setQueryParam(BLOCK_HEIGHT_PARAM, String.valueOf(blockHeight))
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_BAD_REQUEST);

          context.completeNow();
        })));
  }

  @Test
  void getBlockById(VertxTestContext context) {
    Block block = createBlock(1L);

    HashCode blockId = HashCode.fromString(HASH_STRING);
    when(qaService.getBlockById(blockId)).thenReturn(Optional.of(block));

    get(BLOCKCHAIN_BLOCK_PATH).setQueryParam(BLOCK_ID_PARAM, HASH_STRING)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          Object actualBlock = JSON_SERIALIZER
              .fromJson(body, new TypeToken<Block>() {
              }.getType());
          assertThat(actualBlock).isEqualTo(block);

          context.completeNow();
        })));
  }

  @Test
  void getNonexistentBlockById(VertxTestContext context) {
    HashCode blockId = HashCode.fromString(HASH_STRING);
    when(qaService.getBlockById(blockId)).thenReturn(Optional.empty());

    get(BLOCKCHAIN_BLOCK_PATH).setQueryParam(BLOCK_ID_PARAM, HASH_STRING)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_NOT_FOUND);

          context.completeNow();
        })));
  }

  @Test
  void getBlock_InvalidRequest(VertxTestContext context) {
    get(BLOCKCHAIN_BLOCK_PATH)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_BAD_REQUEST);

          context.completeNow();
        })));
  }

  @Test
  void getLastBlock(VertxTestContext context) {
    Block block = createBlock(1L);

    when(qaService.getLastBlock()).thenReturn(block);

    get(BLOCKCHAIN_LAST_BLOCK_PATH)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          Object actualBlock = JSON_SERIALIZER
              .fromJson(body, new TypeToken<Block>() {
              }.getType());
          assertThat(actualBlock).isEqualTo(block);

          context.completeNow();
        })));
  }

  @Test
  void getActualConfiguration(VertxTestContext context) {
    StoredConfiguration configuration = createConfiguration();
    when(qaService.getActualConfiguration()).thenReturn(configuration);

    get(GET_ACTUAL_CONFIGURATION_PATH)
        .send(context.succeeding(response -> context.verify(() -> {
          assertAll(
              () -> assertThat(response.statusCode()).isEqualTo(HTTP_OK),
              () -> {
                String body = response.bodyAsString();
                StoredConfiguration storedConfiguration = JSON_SERIALIZER
                    .fromJson(body, StoredConfiguration.class);

                assertThat(storedConfiguration).isEqualTo(configuration);
              });
          context.completeNow();
        })));
  }

  /**
   * Creates a builder of a block, fully initialized with defaults, inferred from the height.
   * An invocation with the same height will produce exactly the same builder. The block hash
   * is <strong>not</strong> equal to the hash of the block with such parameters.
   *
   * @param blockHeight a block height
   * @return a new block builder
   */
  private static Block createBlock(long blockHeight) {
    HashFunction hashFunction = Hashing.sha256();
    return Block.builder()
        .proposerId(0)
        .height(blockHeight)
        .numTransactions(0)
        .blockHash(hashFunction.hashLong(blockHeight))
        .previousBlockHash(hashFunction.hashLong(blockHeight - 1))
        .txRootHash(hashFunction.hashString("transactions at" + blockHeight, UTF_8))
        .stateHash(hashFunction.hashString("state hash at " + blockHeight, UTF_8))
        .build();
  }

  private StoredConfiguration createConfiguration() {
    return StoredConfiguration.builder()
        .previousCfgHash(HashCode.fromString("11"))
        .actualFrom(1)
        .validatorKeys(
            singletonList(
                ValidatorKey.builder()
                    .consensusKey(PublicKey.fromHexString("22"))
                    .serviceKey(PublicKey.fromHexString("33"))
                    .build()
            )
        )
        .consensusConfiguration(
            ConsensusConfiguration.builder()
                .firstRoundTimeout(1)
                .statusTimeout(2)
                .peersTimeout(3)
                .txsBlockLimit(4)
                .maxMessageLen(5)
                .minProposeTimeout(6)
                .maxProposeTimeout(7)
                .proposeTimeoutThreshold(8)
                .build()
        )
        .build();
  }

  private HttpRequest<Buffer> post(String requestPath) {
    return webClient.post(port, HOST, requestPath);
  }

  private HttpRequest<Buffer> get(String requestPath) {
    return webClient.get(port, HOST, requestPath);
  }

  private static MultiMap multiMap(String k1, String v1, String... entries) {
    checkArgument(entries.length % 2 == 0);

    MultiMap params = MultiMap.caseInsensitiveMultiMap()
        .add(k1, v1);
    int numEntries = entries.length / 2;
    IntStream.range(0, numEntries)
        .forEach(i -> params.add(entries[2 * i], entries[2 * i + 1]));

    return params;
  }

  private String getCounterUri(HashCode id) {
    return getCounterUri(String.valueOf(id));
  }

  private String getCounterUri(String id) {
    try {
      return "/counter/" + URLEncoder.encode(id, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError("UTF-8 must be supported", e);
    }
  }

  private Throwable wrappingChecked(Class<? extends Throwable> checkedException) {
    Throwable wrappingException = logSafeExceptionMock(RuntimeException.class);
    Throwable cause = logSafeExceptionMock(checkedException);
    when(wrappingException.getCause()).thenReturn(cause);
    return wrappingException;
  }

  private Throwable logSafeExceptionMock(Class<? extends Throwable> exceptionType) {
    Throwable t = mock(exceptionType);
    lenient().when(t.getStackTrace()).thenReturn(new StackTraceElement[0]);
    return t;
  }

  private Handler<AsyncResult<HttpResponse<Buffer>>> checkCreatedTransaction(
      VertxTestContext context, HashCode expectedTxHash) {
    return context.succeeding(
        response -> context.verify(() -> {
          assertAll(
              () -> assertThat(response.bodyAsString()).isEqualTo(expectedTxHash.toString()),
              () -> assertThat(response.statusCode()).isEqualTo(HTTP_CREATED),
              () -> assertThat(response.getHeader("Location"))
                  .isEqualTo("/api/explorer/v1/transactions/" + expectedTxHash)
          );
          context.completeNow();
        }));
  }

  private static TransactionMessage transactionMessageSource(String payloadString) {
    CryptoFunction cryptoFunction = CryptoFunctions.ed25519();
    KeyPair keys = cryptoFunction.generateKeyPair();
    return TransactionMessage.builder()
        .serviceId((short) 0)
        .transactionId((short) 1)
        .payload(Bytes.bytes(payloadString))
        .sign(keys, cryptoFunction);
  }
}

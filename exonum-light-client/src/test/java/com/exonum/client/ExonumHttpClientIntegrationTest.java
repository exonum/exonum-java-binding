/*
 * Copyright 2019 The Exonum Team
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
 *
 */

package com.exonum.client;

import static com.exonum.binding.common.crypto.CryptoFunctions.ed25519;
import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static com.exonum.client.ExonumUrls.BLOCK;
import static com.exonum.client.ExonumUrls.HEALTH_CHECK;
import static com.exonum.client.ExonumUrls.MEMORY_POOL;
import static com.exonum.client.ExonumUrls.TRANSACTIONS;
import static com.exonum.client.ExonumUrls.USER_AGENT;
import static com.exonum.client.TestUtils.createTransactionMessage;
import static com.exonum.client.TestUtils.toHex;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.client.ExplorerApiHelper.SubmitTxRequest;
import com.exonum.client.response.BlockResponse;
import com.exonum.client.response.ConsensusStatus;
import com.exonum.client.response.HealthCheckInfo;
import com.exonum.client.response.TransactionResponse;
import com.exonum.client.response.TransactionStatus;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ExonumHttpClientIntegrationTest {
  private static MockWebServer server;
  private static ExonumClient exonumClient;

  @BeforeAll
  static void start() throws IOException {
    server = new MockWebServer();
    server.start();

    exonumClient = ExonumClient.newBuilder()
        .setExonumHost(server.url("/").url())
        .build();
  }

  @AfterAll
  static void shutdown() throws IOException {
    server.shutdown();
  }

  @Test
  void submitTransactionTest() throws InterruptedException {
    // Create request
    KeyPair keys = ed25519().generateKeyPair();
    TransactionMessage txMessage = TransactionMessage.builder()
        .serviceId((short) 1)
        .transactionId((short) 2)
        .payload(new byte[]{0x00, 0x01, 0x02})
        .sign(keys, ed25519());
    // Mock response
    String hash = "f128c720e04b8243";
    String mockResponse = "{\"tx_hash\":\"" + hash + "\"}";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call submitTransaction
    HashCode hashCode = exonumClient.submitTransaction(txMessage);

    // Assert response decoding
    assertThat(hashCode, is(HashCode.fromString(hash)));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("POST"));
    assertThat(recordedRequest.getPath(), is(TRANSACTIONS));

    // Assert request encoding
    String json = recordedRequest.getBody().readUtf8();
    SubmitTxRequest actualRequest = json().fromJson(json, SubmitTxRequest.class);
    TransactionMessage actualTxMessage = actualRequest.getBody();

    assertThat(actualTxMessage, is(txMessage));
  }

  @Test
  void getUnconfirmedTransactions() throws InterruptedException {
    // Mock response
    int mockCount = 10;
    String mockResponse = "{\"size\": " + mockCount + " }";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    int actualCount = exonumClient.getUnconfirmedTransactionsCount();

    // Assert response
    assertThat(actualCount, is(mockCount));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), is(MEMORY_POOL));
  }

  @Test
  void healthCheck() throws InterruptedException {
    // Mock response
    HealthCheckInfo expected = new HealthCheckInfo(ConsensusStatus.ENABLED, 0);
    String mockResponse = "{\"consensus_status\": \"Enabled\", \"connectivity\": \"NotConnected\"}";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    HealthCheckInfo actual = exonumClient.healthCheck();

    // Assert response
    assertThat(actual, is(expected));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), is(HEALTH_CHECK));
  }

  @Test
  void getUserAgentInfo() throws InterruptedException {
    // Mock response
    String mockResponse = "exonum 0.6.0/rustc 1.26.0 (2789b067d 2018-03-06)\n\n/Mac OS10.13.3";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    String actualResponse = exonumClient.getUserAgentInfo();

    // Assert response
    assertThat(actualResponse, is(mockResponse));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), is(USER_AGENT));
  }

  @Test
  void getTransaction() throws InterruptedException {
    // Mock response
    TransactionMessage expectedMessage = createTransactionMessage();
    String mockResponse = "{\n"
        + "    'type': 'in-pool',\n"
        + "    'content': {\n"
        + "        'debug': {\n"
        + "            'to': {\n"
        + "                'data': []\n"
        + "            },\n"
        + "            'amount': 10,\n"
        + "            'seed': 9587307158524814255\n"
        + "        },\n"
        + "        'message': '" + toHex(expectedMessage) + "'\n"
        + "    }\n"
        + "}";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    HashCode id = HashCode.fromInt(0x00);
    Optional<TransactionResponse> response = exonumClient.getTransaction(id);

    // Assert response
    assertTrue(response.isPresent());
    TransactionResponse actualResponse = response.get();
    assertThat(actualResponse.getStatus(), is(TransactionStatus.IN_POOL));
    assertThat(actualResponse.getMessage(), is(expectedMessage));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(TRANSACTIONS));
    assertThat(recordedRequest.getRequestUrl().queryParameter("hash"), is(id.toString()));
  }

  @Test
  void getTransactionNotFound() throws InterruptedException {
    // Mock response
    server.enqueue(new MockResponse().setResponseCode(HTTP_NOT_FOUND));

    // Call
    HashCode id = HashCode.fromInt(0x00);
    Optional<TransactionResponse> response = exonumClient.getTransaction(id);

    // Assert response
    assertFalse(response.isPresent());

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(TRANSACTIONS));
    assertThat(recordedRequest.getRequestUrl().queryParameter("hash"), is(id.toString()));
  }

  @Test
  void getBlockByHeight() throws InterruptedException {
    // Mock response
    String previousHash = "fd510fc923683a4bb77af8278cd51676fbd0fcb25e2437bd69513d468b874bbb";
    String txHash = "336a4acbe2ff0dd18989316f4bc8d17a4bfe79985424fe483c45e8ac92963d13";
    String stateHash = "79a6f0fa233cc2d7d2e96855ec14bdcc4c0e0bb1a99ccaa912a555441e3b7512";
    String tx1 = "336a4acbe2ff0dd18989316f4bc8d17a4bfe79985424fe483c45e8ac92963d13";
    String time = "2019-02-14T14:12:52.037255Z";
    String mockResponse = "{\n"
        + "    \"block\": {\n"
        + "        \"proposer_id\": 3,\n"
        + "        \"height\": 1,\n"
        + "        \"tx_count\": 1,\n"
        + "        \"prev_hash\": \"" + previousHash + "\",\n"
        + "        \"tx_hash\": \"" + txHash + "\",\n"
        + "        \"state_hash\": \"" + stateHash + "\"\n"
        + "    },\n"
        + "    \"precommits\": [\"a410964c2c21199b48e2\"],\n"
        + "    \"txs\": [\"" + tx1 + "\"],\n"
        + "    \"time\": \"" + time + "\"\n"
        + "}";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    long height = Long.MAX_VALUE;
    BlockResponse response = exonumClient.getBlockByHeight(height);

    // Assert response
    assertThat(response.getBlock().getHeight(), is(1L));
    assertThat(response.getBlock().getProposerId(), is(3));
    assertThat(response.getBlock().getNumTransactions(), is(1));
    assertThat(response.getBlock().getPreviousBlockHash(), is(HashCode.fromString(previousHash)));
    assertThat(response.getBlock().getStateHash(), is(HashCode.fromString(stateHash)));
    assertThat(response.getBlock().getTxRootHash(), is(HashCode.fromString(txHash)));
    assertThat(response.getTime(), is(ZonedDateTime.parse(time)));
    assertThat(response.getTransactionHashes(), contains(HashCode.fromString(tx1)));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(BLOCK));
    assertThat(recordedRequest.getRequestUrl().queryParameter("height"),
        is(String.valueOf(height)));
  }

}

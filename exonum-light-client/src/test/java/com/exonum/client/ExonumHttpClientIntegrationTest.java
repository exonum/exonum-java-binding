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
import static com.exonum.client.ExonumApi.JSON;
import static com.exonum.client.RecordedRequestMatchers.hasPath;
import static com.exonum.client.RecordedRequestMatchers.hasQueryParam;
import static com.exonum.client.TestUtils.createTransactionMessage;
import static com.exonum.client.TestUtils.toHex;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.client.ExplorerApiHelper.SubmitTxRequest;
import com.exonum.client.response.ConsensusStatus;
import com.exonum.client.response.HealthCheckInfo;
import com.exonum.client.response.ServiceInfo;
import com.exonum.client.response.TransactionResponse;
import com.exonum.client.response.TransactionStatus;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExonumHttpClientIntegrationTest {

  private static String SERVICE_NAME = "service-name";
  private static int SERVICE_ID = 1;

  private MockWebServer server;
  private ExonumClient exonumClient;

  @BeforeEach
  void start() throws IOException {
    server = new MockWebServer();
    server.start();

    exonumClient = ExonumClient.newBuilder()
        .setExonumHost(server.url("/").url())
        .build();
  }

  @AfterEach
  void shutdown() throws IOException {
    server.shutdown();
  }

  @Test
  void submitTransactionTest() throws InterruptedException {
    // Create request
    KeyPair keys = ed25519().generateKeyPair();
    TransactionMessage txMessage = TransactionMessage.builder()
        .serviceId(1)
        .transactionId(2)
        .payload(new byte[]{0x00, 0x01, 0x02})
        .sign(keys);
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
    assertThat(recordedRequest, hasPath("api/explorer/v1/transactions"));

    // Assert request encoding
    String json = recordedRequest.getBody().readUtf8();
    SubmitTxRequest actualRequest = JSON.fromJson(json, SubmitTxRequest.class);
    TransactionMessage actualTxMessage = actualRequest.getTxBody();

    assertThat(actualTxMessage, is(txMessage));
  }

  @Test
  void getUnconfirmedTransactions() throws InterruptedException {
    // Mock response
    int txPoolSize = 10;
    String mockResponse = "{\"tx_pool_size\": " + txPoolSize + ", \"tx_count\": 1 }";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    int actualCount = exonumClient.getUnconfirmedTransactionsCount();

    // Assert response
    assertThat(actualCount, is(txPoolSize));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest, hasPath("api/system/v1/stats"));
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
    assertThat(recordedRequest, hasPath("api/system/v1/healthcheck"));
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
    assertThat(recordedRequest, hasPath("api/system/v1/user_agent"));
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
    assertThat(recordedRequest, hasPath("api/explorer/v1/transactions"));
    assertThat(recordedRequest, hasQueryParam("hash", id));
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
    assertThat(recordedRequest, hasPath("api/explorer/v1/transactions"));
    assertThat(recordedRequest, hasQueryParam("hash", id));
  }

  @Test
  void getServiceInfoByName() throws InterruptedException {
    ServiceInfo serviceInfo = new ServiceInfo(SERVICE_NAME, SERVICE_ID);
    // Mock response
    String mockResponse = "{\n"
        + "    \"services\": [\n"
        + "      {\n"
        + "          \"name\": \"" + SERVICE_NAME + "\",\n"
        + "          \"id\": " + SERVICE_ID + "\n"
        + "      }\n"
        + "    ]\n"
        + "}";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    Optional<ServiceInfo> response = exonumClient.getServiceInfoByName(SERVICE_NAME);

    // Assert response
    assertTrue(response.isPresent());
    ServiceInfo actualResponse = response.get();
    assertThat(actualResponse, is(serviceInfo));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest, hasPath("api/explorer/v1/services"));
  }

  @Test
  void getServiceInfoByInvalidName() throws InterruptedException {
    // Mock response
    String mockResponse = "{\n"
        + "    \"services\": [\n"
        + "      {\n"
        + "          \"name\": \"" + SERVICE_NAME + "\",\n"
        + "          \"id\": " + SERVICE_ID + "\n"
        + "      }\n"
        + "    ]\n"
        + "}";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    Optional<ServiceInfo> response = exonumClient.getServiceInfoByName("invalid-service-name");

    // Assert response
    assertFalse(response.isPresent());

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest, hasPath("api/explorer/v1/services"));
  }

  @Test
  void getServiceInfoList() throws InterruptedException {
    String serviceName2 = "service-name-2";
    int serviceId2 = 2;
    ServiceInfo serviceInfo1 = new ServiceInfo(SERVICE_NAME, SERVICE_ID);
    ServiceInfo serviceInfo2 = new ServiceInfo(serviceName2, serviceId2);
    // Mock response
    String mockResponse = "{\n"
        + "    \"services\": [\n"
        + "      {\n"
        + "          \"name\": \"" + SERVICE_NAME + "\",\n"
        + "          \"id\": " + SERVICE_ID + "\n"
        + "      },\n"
        + "      {\n"
        + "          \"name\": \"" + serviceName2 + "\",\n"
        + "          \"id\": " + serviceId2 + "\n"
        + "      }\n"
        + "    ]\n"
        + "}";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    List<ServiceInfo> response = exonumClient.getServiceInfoList();

    // Assert response
    assertThat(response, contains(serviceInfo1, serviceInfo2));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest, hasPath("api/explorer/v1/services"));
  }
}

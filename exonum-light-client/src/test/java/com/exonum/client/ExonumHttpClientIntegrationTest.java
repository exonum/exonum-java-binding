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
import com.exonum.client.response.ServiceInstanceInfo;
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

  private static final String SERVICE_NAME = "service-name";
  private static final int SERVICE_ID = 1;

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
  void getTransaction() throws InterruptedException {
    // Mock response
    TransactionMessage expectedMessage = createTransactionMessage();
    String mockResponse = "{\n"
        + "    'type': 'in-pool',\n"
        + "    'message': '" + toHex(expectedMessage) + "'\n"
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
  void findServiceInfo() throws InterruptedException {
    ServiceInstanceInfo serviceInstanceInfo = new ServiceInstanceInfo(SERVICE_NAME, SERVICE_ID);
    // Mock response
    String mockResponse = "{\n"
        + "    \"services\": [{\n"
        + "        \"spec\": {\n"
        + "            \"name\": \"" + SERVICE_NAME + "\",\n"
        + "            \"id\": " + SERVICE_ID + "\n"
        + "            },\n"
        + "            \"status\": \"Active\"\n"
        + "        }\n"
        + "    ]\n"
        + "}";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    Optional<ServiceInstanceInfo> response = exonumClient.findServiceInfo(SERVICE_NAME);

    // Assert response
    assertTrue(response.isPresent());
    ServiceInstanceInfo actualResponse = response.get();
    assertThat(actualResponse, is(serviceInstanceInfo));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest, hasPath("api/services/supervisor/services"));
  }

  @Test
  void findServiceInfoNotFound() throws InterruptedException {
    // Mock response
    String mockResponse = "{\n"
        + "    \"services\": [{\n"
        + "        \"spec\": {\n"
        + "            \"name\": \"" + SERVICE_NAME + "\",\n"
        + "            \"id\": " + SERVICE_ID + "\n"
        + "            },\n"
        + "            \"status\": \"Active\"\n"
        + "        }\n"
        + "    ]\n"
        + "}";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    Optional<ServiceInstanceInfo> response = exonumClient.findServiceInfo("invalid-service-name");

    // Assert response
    assertFalse(response.isPresent());

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest, hasPath("api/services/supervisor/services"));
  }

  @Test
  void getServiceInfoList() throws InterruptedException {
    String serviceName2 = "service-name-2";
    int serviceId2 = 2;
    ServiceInstanceInfo serviceInstanceInfo1 = new ServiceInstanceInfo(SERVICE_NAME, SERVICE_ID);
    ServiceInstanceInfo serviceInstanceInfo2 = new ServiceInstanceInfo(serviceName2, serviceId2);
    // Mock response
    String mockResponse = "{\n"
        + "    \"services\": [{\n"
        + "        \"spec\": {\n"
        + "            \"name\": \"" + SERVICE_NAME + "\",\n"
        + "            \"id\": " + SERVICE_ID + "\n"
        + "            },\n"
        + "            \"status\": \"Active\"\n"
        + "        },\n"
        + "        {\n"
        + "        \"spec\": {\n"
        + "            \"name\": \"" + serviceName2 + "\",\n"
        + "            \"id\": " + serviceId2 + "\n"
        + "            },\n"
        + "            \"status\": \"Active\"\n"
        + "        }\n"
        + "    ]\n"
        + "}";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    List<ServiceInstanceInfo> response = exonumClient.getServiceInfoList();

    // Assert response
    assertThat(response, contains(serviceInstanceInfo1, serviceInstanceInfo2));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest, hasPath("api/services/supervisor/services"));
  }
}

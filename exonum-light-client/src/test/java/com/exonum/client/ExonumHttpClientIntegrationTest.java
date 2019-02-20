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
import static com.exonum.client.ExonumUrls.HEALTH_CHECK;
import static com.exonum.client.ExonumUrls.MEMORY_POOL;
import static com.exonum.client.ExonumUrls.TRANSACTIONS;
import static com.exonum.client.ExonumUrls.USER_AGENT;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.client.response.ConsensusStatus;
import com.exonum.client.response.HealthCheckInfo;
import com.exonum.client.response.TransactionResponse;
import java.io.IOException;
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
  void getTransactionNotFound() throws InterruptedException {
    // Mock response
    server.enqueue(new MockResponse().setResponseCode(HTTP_NOT_FOUND));

    // Call
    Optional<TransactionResponse> response = exonumClient.getTransaction(HashCode.fromInt(0x00));

    // Assert response
    assertFalse(response.isPresent());

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), is(TRANSACTIONS));
  }

}

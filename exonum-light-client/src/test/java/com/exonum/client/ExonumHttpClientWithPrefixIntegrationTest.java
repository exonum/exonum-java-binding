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
 */

package com.exonum.client;

import static com.exonum.client.TestUtils.assertPath;

import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExonumHttpClientWithPrefixIntegrationTest {
  private MockWebServer server;
  private ExonumClient exonumClient;
  private String prefixUrl = "pre/fix";

  @BeforeEach
  void start() throws IOException {
    server = new MockWebServer();
    server.start();

    exonumClient = ExonumClient.newBuilder()
        .setExonumHost(server.url("/").url())
        .setPrefix(prefixUrl)
        .build();
  }

  @AfterEach
  void shutdown() throws IOException {
    server.shutdown();
  }

  @Test
  @DisplayName("LC applies the given prefix to the underlying requests")
  void requestWithPrefix() throws InterruptedException {
    // Mock response
    server.enqueue(new MockResponse().setBody("ok"));

    // Call
    exonumClient.getUserAgentInfo();

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertPath(recordedRequest, prefixUrl);
  }

}

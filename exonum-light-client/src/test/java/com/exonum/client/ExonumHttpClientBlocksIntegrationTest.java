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

import static com.exonum.client.ExonumApi.MAX_BLOCKS_PER_REQUEST;
import static com.exonum.client.ExonumUrls.BLOCK;
import static com.exonum.client.ExonumUrls.BLOCKS;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.client.response.Block;
import com.exonum.client.response.BlockResponse;
import com.exonum.client.response.BlocksResponse;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExonumHttpClientBlocksIntegrationTest {
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

    Block expectedBlock = Block.builder()
        .height(1L)
        .proposerId(3)
        .numTransactions(1)
        .previousBlockHash(HashCode.fromString(previousHash))
        .stateHash(HashCode.fromString(stateHash))
        .txRootHash(HashCode.fromString(txHash))
        .build();
    // Assert response
    assertThat(response.getBlock(), is(expectedBlock));
    assertThat(response.getTime(), is(ZonedDateTime.parse(time)));
    assertThat(response.getTransactionHashes(), contains(HashCode.fromString(tx1)));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(BLOCK));
    assertThat(recordedRequest.getRequestUrl().queryParameter("height"),
        is(String.valueOf(height)));
  }

  @Test
  void getBlockNotFound() {
    server.enqueue(new MockResponse().setResponseCode(HTTP_NOT_FOUND));

    assertThrows(RuntimeException.class, () -> exonumClient.getBlockByHeight(1L));
  }

  @ParameterizedTest
  @ValueSource(longs = {Long.MIN_VALUE, -1L})
  void getBlockWrongHeight(long height) {
    assertThrows(IllegalArgumentException.class, () -> exonumClient.getBlockByHeight(height));
  }

  @Test
  void getBlocks() throws InterruptedException {
    String mockResponse = "{\n"
        + "    \"range\": {\n"
        + "        \"start\": 6,\n"
        + "        \"end\": 288\n"
        + "    },\n"
        + "    \"blocks\": [{\n"
        + "        \"proposer_id\": 3,\n"
        + "        \"height\": 26,\n"
        + "        \"tx_count\": 1,\n"
        + "        \"prev_hash\": \"932470a22d37a5a995519e01c50eab7db9e0\",\n"
        + "        \"tx_hash\": \"5cc41a2a7cf7c0d3a15ab6ca775b601208dba7\",\n"
        + "        \"state_hash\": \"4d7bb34d7913e0784c24a1e440532e7290\"\n"
        + "    }, {\n"
        + "        \"proposer_id\": 3,\n"
        + "        \"height\": 6,\n"
        + "        \"tx_count\": 1,\n"
        + "        \"prev_hash\": \"dbec8f64a85ab56985c7ab7e63a191764f4d\",\n"
        + "        \"tx_hash\": \"ffee3d630f137aecff95aece36cfe4dc1b42f6\",\n"
        + "        \"state_hash\": \"8ac9f2af6266b8e9b61fa7f3fcdd170375\"\n"
        + "    }],\n"
        + "    \"times\": [\"2019-02-21T13:01:44.321051Z\", \n"
        + "             \"2019-02-21T13:01:40.199265Z\"]\n"
        + "}\n";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    int blocksCount = MAX_BLOCKS_PER_REQUEST;
    boolean skipEmpty = true;
    long height = Long.MAX_VALUE;
    boolean showTimes = true;
    BlocksResponse response = exonumClient.getBlocks(blocksCount, skipEmpty, height, showTimes);

    // Assert response
    assertThat(response.getBlocks(), hasSize(2));
    assertThat(response.getBlockCommitTimes(), hasSize(2));
    assertThat(response.getBlocksRangeStart(), is(6L));
    assertThat(response.getBlocksRangeEnd(), is(288L));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(BLOCKS));
    assertBlockRequestParams(recordedRequest, blocksCount, skipEmpty, height, showTimes);
  }

  @Test
  void getBlocksNoTime() throws InterruptedException {
    String mockResponse = "{\n"
        + "    \"range\": {\n"
        + "        \"start\": 6,\n"
        + "        \"end\": 288\n"
        + "    },\n"
        + "    \"blocks\": [{\n"
        + "        \"proposer_id\": 3,\n"
        + "        \"height\": 26,\n"
        + "        \"tx_count\": 1,\n"
        + "        \"prev_hash\": \"932470a22d37a5a995519e01c50eab7db9e0\",\n"
        + "        \"tx_hash\": \"5cc41a2a7cf7c0d3a15ab6ca775b601208dba7\",\n"
        + "        \"state_hash\": \"4d7bb34d7913e0784c24a1e440532e7290\"\n"
        + "    }, {\n"
        + "        \"proposer_id\": 3,\n"
        + "        \"height\": 6,\n"
        + "        \"tx_count\": 1,\n"
        + "        \"prev_hash\": \"dbec8f64a85ab56985c7ab7e63a191764f4d\",\n"
        + "        \"tx_hash\": \"ffee3d630f137aecff95aece36cfe4dc1b42f6\",\n"
        + "        \"state_hash\": \"8ac9f2af6266b8e9b61fa7f3fcdd170375\"\n"
        + "    }],\n"
        + "    \"times\": null\n"
        + "}\n";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    int blocksCount = MAX_BLOCKS_PER_REQUEST;
    boolean skipEmpty = true;
    long height = Long.MAX_VALUE;
    boolean showTimes = false;
    BlocksResponse response = exonumClient.getBlocks(blocksCount, skipEmpty, height, showTimes);

    // Assert response
    assertThat(response.getBlocks(), hasSize(2));
    assertThat(response.getBlockCommitTimes(), empty());
    assertThat(response.getBlocksRangeStart(), is(6L));
    assertThat(response.getBlocksRangeEnd(), is(288L));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(BLOCKS));
    assertBlockRequestParams(recordedRequest, blocksCount, skipEmpty, height, showTimes);
  }

  @ParameterizedTest
  @ValueSource(ints = {Integer.MIN_VALUE, -1, MAX_BLOCKS_PER_REQUEST + 1, Integer.MAX_VALUE})
  void getBlocksWrongBlocksCount(int blocksCount) {
    assertThrows(IllegalArgumentException.class,
        () -> exonumClient.getBlocks(blocksCount, false, 1L, false));
  }

  @ParameterizedTest
  @ValueSource(longs = {Long.MIN_VALUE, -1L})
  void getBlocksWrongHeight(long heightMax) {
    assertThrows(IllegalArgumentException.class,
        () -> exonumClient.getBlocks(1, false, heightMax, false));
  }

  @Test
  void getLastBlocks() throws InterruptedException {
    String mockResponse = "{\n"
        + "    \"range\": {\n"
        + "        \"start\": 6,\n"
        + "        \"end\": 288\n"
        + "    },\n"
        + "    \"blocks\": [{\n"
        + "        \"proposer_id\": 3,\n"
        + "        \"height\": 26,\n"
        + "        \"tx_count\": 1,\n"
        + "        \"prev_hash\": \"932470a22d37a5a995519e01c50eab7db9e0\",\n"
        + "        \"tx_hash\": \"5cc41a2a7cf7c0d3a15ab6ca775b601208dba7\",\n"
        + "        \"state_hash\": \"4d7bb34d7913e0784c24a1e440532e7290\"\n"
        + "    }, {\n"
        + "        \"proposer_id\": 3,\n"
        + "        \"height\": 6,\n"
        + "        \"tx_count\": 1,\n"
        + "        \"prev_hash\": \"dbec8f64a85ab56985c7ab7e63a191764f4d\",\n"
        + "        \"tx_hash\": \"ffee3d630f137aecff95aece36cfe4dc1b42f6\",\n"
        + "        \"state_hash\": \"8ac9f2af6266b8e9b61fa7f3fcdd170375\"\n"
        + "    }],\n"
        + "    \"times\": [\"2019-02-21T13:01:44.321051Z\", \n"
        + "             \"2019-02-21T13:01:40.199265Z\"]\n"
        + "}\n";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    int blocksCount = MAX_BLOCKS_PER_REQUEST;
    boolean skipEmpty = true;
    boolean showTimes = true;
    BlocksResponse response = exonumClient.getLastBlocks(blocksCount, skipEmpty, showTimes);

    // Assert response
    assertThat(response.getBlocks(), hasSize(2));
    assertThat(response.getBlockCommitTimes(), hasSize(2));
    assertThat(response.getBlocksRangeStart(), is(6L));
    assertThat(response.getBlocksRangeEnd(), is(288L));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(BLOCKS));
    assertBlockRequestParams(recordedRequest, blocksCount, skipEmpty, null, showTimes);
  }

  @Test
  void getLastBlock() throws InterruptedException {
    String mockResponse = "{\n"
        + "    \"range\": {\n"
        + "        \"start\": 6,\n"
        + "        \"end\": 7\n"
        + "    },\n"
        + "    \"blocks\": [{\n"
        + "        \"proposer_id\": 3,\n"
        + "        \"height\": 26,\n"
        + "        \"tx_count\": 1,\n"
        + "        \"prev_hash\": \"932470a22d37a5a995519e01c50eab7db9e0\",\n"
        + "        \"tx_hash\": \"5cc41a2a7cf7c0d3a15ab6ca775b601208dba7\",\n"
        + "        \"state_hash\": \"4d7bb34d7913e0784c24a1e440532e7290\"\n"
        + "    }],\n"
        + "    \"times\": null\n"
        + "}\n";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    Block block = exonumClient.getLastBlock();

    // Assert response
    assertThat(block, notNullValue());

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(BLOCKS));
    assertBlockRequestParams(recordedRequest, 1, false, null, false);
  }

  @Test
  void getLastNotEmptyBlock() throws InterruptedException {
    String mockResponse = "{\n"
        + "    \"range\": {\n"
        + "        \"start\": 6,\n"
        + "        \"end\": 7\n"
        + "    },\n"
        + "    \"blocks\": [{\n"
        + "        \"proposer_id\": 3,\n"
        + "        \"height\": 26,\n"
        + "        \"tx_count\": 1,\n"
        + "        \"prev_hash\": \"932470a22d37a5a995519e01c50eab7db9e0\",\n"
        + "        \"tx_hash\": \"5cc41a2a7cf7c0d3a15ab6ca775b601208dba7\",\n"
        + "        \"state_hash\": \"4d7bb34d7913e0784c24a1e440532e7290\"\n"
        + "    }],\n"
        + "    \"times\": null\n"
        + "}\n";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    Optional<Block> block = exonumClient.getLastNonEmptyBlock();

    // Assert response
    assertTrue(block.isPresent());

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(BLOCKS));
    assertBlockRequestParams(recordedRequest, 1, true, null, false);
  }

  @Test
  void getLastNotEmptyBlockNoBlock() throws InterruptedException {
    String mockResponse = "{\n"
        + "    \"range\": {\n"
        + "        \"start\": 6,\n"
        + "        \"end\": 7\n"
        + "    },\n"
        + "    \"blocks\": [],\n"
        + "    \"times\": null\n"
        + "}\n";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    Optional<Block> block = exonumClient.getLastNonEmptyBlock();

    // Assert response
    assertFalse(block.isPresent());

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(BLOCKS));
    assertBlockRequestParams(recordedRequest, 1, true, null, false);
  }

  @Test
  void getBlockchainHeight() throws InterruptedException {
    // Mock response
    long height = 1600;
    String json = "{\n"
        + "  \"range\": {\n"
        + "    \"start\": 0,\n"
        + "    \"end\": " + height + "\n"
        + "  },\n"
        + "  \"blocks\": [],\n"
        + "  \"times\": null\n"
        + "}\n";
    server.enqueue(new MockResponse().setBody(json));

    // Call
    long actualHeight = exonumClient.getBlockchainHeight();

    // Assert response
    assertThat(actualHeight, is(height));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(BLOCKS));
    assertBlockRequestParams(recordedRequest, 0, false, null, false);
  }

  private static void assertBlockRequestParams(RecordedRequest request, int count,
      boolean skipEmpty, Long heightMax, boolean withBlocksTime) {
    assertThat(request.getRequestUrl().queryParameter("count"),
        is(String.valueOf(count)));
    assertThat(request.getRequestUrl().queryParameter("skip_empty_blocks"),
        is(String.valueOf(skipEmpty)));
    if (heightMax == null) {
      assertThat(request.getRequestUrl().queryParameter("latest"), nullValue());
    } else {
      assertThat(request.getRequestUrl().queryParameter("latest"),
          is(String.valueOf(heightMax)));
    }
    assertThat(request.getRequestUrl().queryParameter("add_blocks_time"),
        is(String.valueOf(withBlocksTime)));
  }

}

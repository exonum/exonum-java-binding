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

import static com.exonum.client.Blocks.BLOCK_1;
import static com.exonum.client.Blocks.BLOCK_1_JSON;
import static com.exonum.client.Blocks.BLOCK_1_TIME;
import static com.exonum.client.Blocks.BLOCK_1_WITHOUT_TIME;
import static com.exonum.client.Blocks.BLOCK_2;
import static com.exonum.client.Blocks.BLOCK_2_JSON;
import static com.exonum.client.Blocks.BLOCK_2_TIME;
import static com.exonum.client.Blocks.BLOCK_2_WITHOUT_TIME;
import static com.exonum.client.Blocks.BLOCK_3;
import static com.exonum.client.Blocks.BLOCK_3_JSON;
import static com.exonum.client.Blocks.BLOCK_3_TIME;
import static com.exonum.client.Blocks.BLOCK_3_WITHOUT_TIME;
import static com.exonum.client.ExonumApi.MAX_BLOCKS_PER_REQUEST;
import static com.exonum.client.ExonumUrls.BLOCK;
import static com.exonum.client.ExonumUrls.BLOCKS;
import static com.exonum.client.request.BlockFilteringOption.INCLUDE_EMPTY;
import static com.exonum.client.request.BlockFilteringOption.SKIP_EMPTY;
import static com.exonum.client.request.BlockTimeOption.INCLUDE_COMMIT_TIME;
import static com.exonum.client.request.BlockTimeOption.NO_COMMIT_TIME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.client.request.BlockFilteringOption;
import com.exonum.client.request.BlockTimeOption;
import com.exonum.client.response.Block;
import com.exonum.client.response.BlockResponse;
import com.exonum.client.response.BlocksResponse;
import java.io.IOException;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExonumHttpClientBlocksIntegrationTest {
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
  void getBlockByHeight() throws InterruptedException {
    // Mock response
    String tx1 = "336a4acbe2ff0dd18989316f4bc8d17a4bfe79985424fe483c45e8ac92963d13";
    String mockResponse = "{\n"
        + "    'block': " + BLOCK_1_JSON + ",\n"
        + "    'precommits': ['a410964c2c21199b48e2'],\n"
        + "    'txs': ['" + tx1 + "'],\n"
        + "    'time': '" + BLOCK_1_TIME + "'\n"
        + "}";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    long height = Long.MAX_VALUE;
    BlockResponse response = exonumClient.getBlockByHeight(height);

    // Assert response
    assertThat(response.getBlock(), is(BLOCK_1));
    assertThat(response.getTransactionHashes(), contains(HashCode.fromString(tx1)));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(BLOCK));
    assertThat(recordedRequest.getRequestUrl().queryParameter("height"),
        is(String.valueOf(height)));
  }

  @ParameterizedTest
  @ValueSource(longs = {Long.MIN_VALUE, -1L})
  void getBlockWrongHeight(long height) {
    assertThrows(IllegalArgumentException.class, () -> exonumClient.getBlockByHeight(height));
  }

  @Test
  void getBlocks() throws InterruptedException {
    String mockResponse = "{\n"
        + "    'range': {\n"
        + "        'start': 6,\n"
        + "        'end': 288\n"
        + "    },\n"
        + "    'blocks': [ " + BLOCK_1_JSON + "," + BLOCK_2_JSON + "," + BLOCK_3_JSON + "],\n"
        + "    'times': ['" + BLOCK_1_TIME + "','" + BLOCK_2_TIME + "','" + BLOCK_3_TIME + "']\n"
        + "}\n";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    int blocksCount = MAX_BLOCKS_PER_REQUEST;
    BlockFilteringOption blockFilter = SKIP_EMPTY;
    long height = Long.MAX_VALUE;
    BlockTimeOption timeOption = INCLUDE_COMMIT_TIME;
    BlocksResponse response = exonumClient.getBlocks(blocksCount, blockFilter, height, timeOption);

    // Assert response
    assertThat(response.getBlocks(), contains(BLOCK_1, BLOCK_2, BLOCK_3));
    assertThat(response.getBlocksRangeStart(), is(6L));
    assertThat(response.getBlocksRangeEnd(), is(288L));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(BLOCKS));
    assertBlockRequestParams(recordedRequest, blocksCount, blockFilter, height, timeOption);
  }

  @Test
  void getBlocksNoTime() throws InterruptedException {
    String mockResponse = "{\n"
        + "    'range': {\n"
        + "        'start': 6,\n"
        + "        'end': 288\n"
        + "    },\n"
        + "    'blocks': [ " + BLOCK_1_JSON + "," + BLOCK_2_JSON + "," + BLOCK_3_JSON + "],\n"
        + "    'times': null\n"
        + "}\n";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    int blocksCount = MAX_BLOCKS_PER_REQUEST;
    BlockFilteringOption blockFilter = SKIP_EMPTY;
    long height = Long.MAX_VALUE;
    BlockTimeOption timeOption = NO_COMMIT_TIME;
    BlocksResponse response = exonumClient.getBlocks(blocksCount, blockFilter, height, timeOption);

    // Assert response
    assertThat(response.getBlocks(),
        contains(BLOCK_1_WITHOUT_TIME, BLOCK_2_WITHOUT_TIME, BLOCK_3_WITHOUT_TIME));
    assertThat(response.getBlocksRangeStart(), is(6L));
    assertThat(response.getBlocksRangeEnd(), is(288L));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(BLOCKS));
    assertBlockRequestParams(recordedRequest, blocksCount, blockFilter, height, timeOption);
  }

  @ParameterizedTest
  @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, MAX_BLOCKS_PER_REQUEST + 1, Integer.MAX_VALUE})
  void getBlocksWrongBlocksCount(int blocksCount) {
    assertThrows(IllegalArgumentException.class,
        () -> exonumClient.getBlocks(blocksCount, INCLUDE_EMPTY, 1L, NO_COMMIT_TIME));
  }

  @ParameterizedTest
  @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, MAX_BLOCKS_PER_REQUEST + 1, Integer.MAX_VALUE})
  void getLastBlocksWrongBlocksCount(int blocksCount) {
    assertThrows(IllegalArgumentException.class,
        () -> exonumClient.getLastBlocks(blocksCount, INCLUDE_EMPTY, NO_COMMIT_TIME));
  }

  @ParameterizedTest
  @ValueSource(longs = {Long.MIN_VALUE, -1L})
  void getBlocksWrongHeight(long heightMax) {
    assertThrows(IllegalArgumentException.class,
        () -> exonumClient.getBlocks(1, INCLUDE_EMPTY, heightMax, NO_COMMIT_TIME));
  }

  @Test
  void getLastBlocks() throws InterruptedException {
    String mockResponse = "{\n"
        + "    'range': {\n"
        + "        'start': 6,\n"
        + "        'end': 288\n"
        + "    },\n"
        + "    'blocks': [ " + BLOCK_1_JSON + "," + BLOCK_2_JSON + "," + BLOCK_3_JSON + "],\n"
        + "    'times': ['" + BLOCK_1_TIME + "','" + BLOCK_2_TIME + "','" + BLOCK_3_TIME + "']\n"
        + "}\n";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    int blocksCount = MAX_BLOCKS_PER_REQUEST;
    BlockFilteringOption blockFilter = SKIP_EMPTY;
    BlockTimeOption timeOption = INCLUDE_COMMIT_TIME;
    BlocksResponse response = exonumClient.getLastBlocks(blocksCount, blockFilter, timeOption);

    // Assert response
    assertThat(response.getBlocks(), contains(BLOCK_1, BLOCK_2, BLOCK_3));
    assertThat(response.getBlocksRangeStart(), is(6L));
    assertThat(response.getBlocksRangeEnd(), is(288L));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(BLOCKS));
    assertBlockRequestParams(recordedRequest, blocksCount, blockFilter, null, timeOption);
  }

  @Test
  void getLastBlock() throws InterruptedException {
    String mockResponse = "{\n"
        + "    'range': {\n"
        + "        'start': 6,\n"
        + "        'end': 7\n"
        + "    },\n"
        + "    'blocks': [ " + BLOCK_1_JSON + "],\n"
        + "    'times': null\n"
        + "}\n";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    Block block = exonumClient.getLastBlock();

    // Assert response
    assertThat(block, is(BLOCK_1_WITHOUT_TIME));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(BLOCKS));
    assertBlockRequestParams(recordedRequest, 1, INCLUDE_EMPTY, null, INCLUDE_COMMIT_TIME);
  }

  @Test
  void getLastNotEmptyBlock() throws InterruptedException {
    String mockResponse = "{\n"
        + "    'range': {\n"
        + "        'start': 6,\n"
        + "        'end': 7\n"
        + "    },\n"
        + "    'blocks': [ " + BLOCK_1_JSON + "],\n"
        + "    'times': null\n"
        + "}\n";
    server.enqueue(new MockResponse().setBody(mockResponse));

    // Call
    Optional<Block> block = exonumClient.getLastNonEmptyBlock();

    // Assert response
    assertTrue(block.isPresent());
    assertThat(block.get(), is(BLOCK_1_WITHOUT_TIME));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(BLOCKS));
    assertBlockRequestParams(recordedRequest, 1, SKIP_EMPTY, null, INCLUDE_COMMIT_TIME);
  }

  @Test
  void getLastNotEmptyBlockNoBlock() throws InterruptedException {
    String mockResponse = "{\n"
        + "    'range': {\n"
        + "        'start': 6,\n"
        + "        'end': 7\n"
        + "    },\n"
        + "    'blocks': [],\n"
        + "    'times': null\n"
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
    assertBlockRequestParams(recordedRequest, 1, SKIP_EMPTY, null, INCLUDE_COMMIT_TIME);
  }

  @Test
  void getBlockchainHeight() throws InterruptedException {
    // Mock response
    long height = 1600;
    long heightExclusive = height + 1;
    String json = "{\n"
        + "  'range': {\n"
        + "    'start': 0,\n"
        + "    'end': " + heightExclusive + "\n"
        + "  },\n"
        + "  'blocks': [],\n"
        + "  'times': null\n"
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
    assertBlockRequestParams(recordedRequest, 0, INCLUDE_EMPTY, null, NO_COMMIT_TIME);
  }

  private static void assertBlockRequestParams(RecordedRequest request, int count,
      BlockFilteringOption blockFilter, Long heightMax, BlockTimeOption timeOption) {
    boolean skipEmpty = blockFilter == SKIP_EMPTY;
    boolean withTime = timeOption == INCLUDE_COMMIT_TIME;

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
        is(String.valueOf(withTime)));
  }

}

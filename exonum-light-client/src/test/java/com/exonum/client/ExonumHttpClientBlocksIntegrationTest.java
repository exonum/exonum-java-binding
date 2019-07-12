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
import static com.exonum.client.Blocks.aBlock;
import static com.exonum.client.ExonumApi.JSON;
import static com.exonum.client.ExonumApi.MAX_BLOCKS_PER_REQUEST;
import static com.exonum.client.ExonumUrls.BLOCK;
import static com.exonum.client.ExonumUrls.BLOCKS;
import static com.exonum.client.TestUtils.assertPath;
import static com.exonum.client.request.BlockFilteringOption.INCLUDE_EMPTY;
import static com.exonum.client.request.BlockFilteringOption.SKIP_EMPTY;
import static com.exonum.client.request.BlockTimeOption.INCLUDE_COMMIT_TIME;
import static com.exonum.client.request.BlockTimeOption.NO_COMMIT_TIME;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Comparators.isInStrictOrder;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.lang.Math.min;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.client.ExplorerApiHelper.GetBlockResponseBlock;
import com.exonum.client.request.BlockFilteringOption;
import com.exonum.client.request.BlockTimeOption;
import com.exonum.client.response.Block;
import com.exonum.client.response.BlockResponse;
import com.exonum.client.response.BlocksRange;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    enqueueResponse(mockResponse);

    // Call
    long height = BLOCK_1.getHeight();
    BlockResponse response = exonumClient.getBlockByHeight(height);

    // Assert response
    assertThat(response.getBlock(), is(BLOCK_1));
    assertThat(response.getTransactionHashes(), contains(HashCode.fromString(tx1)));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertPath(recordedRequest, BLOCK);
    assertThat(recordedRequest.getRequestUrl().queryParameter("height"),
        is(String.valueOf(height)));
  }

  @ParameterizedTest
  @ValueSource(longs = {Long.MIN_VALUE, -1L})
  void getBlockWrongHeight(long height) {
    assertThrows(IllegalArgumentException.class, () -> exonumClient.getBlockByHeight(height));
  }

  @Test
  void getBlocksSinglePageSkippingEmpty() throws InterruptedException {
    long fromHeight = BLOCK_1.getHeight();
    long toHeight = BLOCK_3.getHeight();
    String mockResponse = "{\n"
        + "    'range': {\n"
        + "        'start': " + fromHeight + ",\n"
        + "        'end': " + toHeight + 1 + "\n"
        + "    },\n"
        + "    'blocks': [ " + BLOCK_3_JSON + "," + BLOCK_2_JSON + "," + BLOCK_1_JSON + "],\n"
        + "    'times': ['" + BLOCK_3_TIME + "','" + BLOCK_2_TIME + "','" + BLOCK_1_TIME + "']\n"
        + "}\n";
    enqueueResponse(mockResponse);

    // Call
    BlockFilteringOption blockFilter = SKIP_EMPTY;
    BlockTimeOption timeOption = INCLUDE_COMMIT_TIME;
    List<Block> blocks = exonumClient.getBlocks(fromHeight, toHeight, blockFilter, timeOption);

    // Assert response
    List<Block> expected = of(BLOCK_1, BLOCK_2, BLOCK_3);
    assertThat(blocks, equalTo(expected));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertPath(recordedRequest, BLOCKS);
    int expectedNumBlocks = Math.toIntExact(toHeight - fromHeight + 1);
    assertBlockRequestParams(recordedRequest, expectedNumBlocks, blockFilter, toHeight, timeOption);
  }

  @Test
  void getBlocksSinglePageSkippingEmptyFiltersOutOfRangeBlocks() throws InterruptedException {
    // 'start' is less than requested 'from' because the requests to core specify the number
    // of blocks to return, not the 'from', hence the Exonum response might include some blocks
    // before the intended 'from', which we would like to test.
    long start = 800;
    long fromHeight = 1000;
    long toHeight = 1100;
    // A single block in the range
    // NB: The response below is not 100% accurate, as Exonum would return the 'count' blocks
    // (or the total number of empty blocks in the blockchain at or below the 'fromHeight')
    List<Block> inRangeBlocks = ImmutableList.of(aBlock()
        .height(1050)
        .build());
    List<Block> outOfRangeBlocks = ImmutableList.of(aBlock()
        .height(850)
        .build());
    List<Block> responseBlocks = concatLists(outOfRangeBlocks, inRangeBlocks);
    String mockResponse = createGetBlocksResponse(start, toHeight + 1, responseBlocks);
    enqueueResponse(mockResponse);

    // Call
    BlockFilteringOption blockFilter = SKIP_EMPTY;
    BlockTimeOption timeOption = NO_COMMIT_TIME;
    List<Block> blocks = exonumClient.getBlocks(fromHeight, toHeight, blockFilter, timeOption);

    // Assert response
    assertThat(blocks, equalTo(inRangeBlocks));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertPath(recordedRequest, BLOCKS);
    int expectedNumBlocks = Math.toIntExact(toHeight - fromHeight + 1);
    assertBlockRequestParams(recordedRequest, expectedNumBlocks, blockFilter, toHeight, timeOption);
  }

  @Test
  void getBlocksSinglePageNoTime() throws InterruptedException {
    long fromHeight = BLOCK_1.getHeight();
    long toHeight = BLOCK_3.getHeight();
    String mockResponse = "{\n"
        + "    'range': {\n"
        + "        'start': " + fromHeight + ",\n"
        + "        'end': " + toHeight + 1 + "\n"
        + "    },\n"
        + "    'blocks': [ " + BLOCK_3_JSON + "," + BLOCK_2_JSON + "," + BLOCK_1_JSON + "],\n"
        + "    'times': null\n"
        + "}\n";
    enqueueResponse(mockResponse);

    // Call
    BlockFilteringOption blockFilter = SKIP_EMPTY;
    BlockTimeOption timeOption = NO_COMMIT_TIME;
    List<Block> blocks = exonumClient.getBlocks(fromHeight, toHeight, blockFilter, timeOption);

    // Assert response
    List<Block> expected = of(BLOCK_1_WITHOUT_TIME, BLOCK_2_WITHOUT_TIME, BLOCK_3_WITHOUT_TIME);
    assertThat(blocks, equalTo(expected));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertPath(recordedRequest, BLOCKS);
    int expectedNumBlocks = Math.toIntExact(toHeight - fromHeight + 1);
    assertBlockRequestParams(recordedRequest, expectedNumBlocks, blockFilter, toHeight, timeOption);
  }

  @Test
  void getBlocksMultiplePagesSkippingEmpty() throws Exception {
    long fromHeight = 100;
    long toHeight = 1200;

    // NB: These responses are not realistic because they include only blocks in the given
    //   range, whilst the request asks for MAX_BLOCKS_PER_REQUEST non-empty blocks, and
    //   Exonum will return these two blocks in a single request.
    Block firstPageBlock = aBlock()
        .height(1100)
        .build();
    long startP1 = toHeight - MAX_BLOCKS_PER_REQUEST + 1;
    long endP1 = toHeight + 1;
    String firstResponse = createGetBlocksResponse(startP1, endP1,
        ImmutableList.of(firstPageBlock));
    enqueueResponse(firstResponse);

    Block secondPageBlock = aBlock()
        .height(102)
        .build();
    String secondResponse = createGetBlocksResponse(fromHeight, startP1,
        ImmutableList.of(secondPageBlock));
    enqueueResponse(secondResponse);

    // Call
    BlockFilteringOption blockFilter = SKIP_EMPTY;
    BlockTimeOption timeOption = NO_COMMIT_TIME;
    List<Block> blocks = exonumClient.getBlocks(fromHeight, toHeight, blockFilter, timeOption);

    // Check response
    List<Block> expected = of(secondPageBlock, firstPageBlock);
    assertThat(blocks, equalTo(expected));

    // Check the requests made
    RecordedRequest firstRequest = server.takeRequest();
    assertBlockRequestParams(firstRequest, MAX_BLOCKS_PER_REQUEST, blockFilter, toHeight,
        timeOption);

    RecordedRequest secondRequest = server.takeRequest();
    assertBlockRequestParams(secondRequest, 101, blockFilter, (startP1 - 1),
        timeOption);
  }

  @ParameterizedTest(name = "[{index}] 2nd page range: [{0}, 1999]")
  @ValueSource(longs = {1000, 1998, 1999})
  void getBlocksMultiplePagesWithEmpty(long fromHeight) throws Exception {
    // Request a range [<from <= 1999>, 2999] spanning two pages: [<from>, 1999], [2000, 2999]
    long toHeight = 2999;
    long startP1 = toHeight - MAX_BLOCKS_PER_REQUEST + 1;
    long toP2 = startP1 - 1;
    List<Block> page1Blocks = createBlocks(startP1, toHeight);
    List<Block> page2Blocks = createBlocks(fromHeight, toP2);
    String firstResponse = createGetBlocksResponseWithEmpty(page1Blocks);
    String secondResponse = createGetBlocksResponseWithEmpty(page2Blocks);
    enqueueResponses(firstResponse, secondResponse);

    // Call
    BlockFilteringOption blockFilter = INCLUDE_EMPTY;
    BlockTimeOption timeOption = NO_COMMIT_TIME;
    List<Block> blocks = exonumClient.getBlocks(fromHeight, toHeight, blockFilter, timeOption);

    List<Block> expectedBlocks = concatLists(page2Blocks, page1Blocks);
    assertThat(blocks, equalTo(expectedBlocks));

    // Check the requests made
    RecordedRequest firstRequest = server.takeRequest();
    assertBlockRequestParams(firstRequest, MAX_BLOCKS_PER_REQUEST, blockFilter, toHeight,
        timeOption);

    RecordedRequest secondRequest = server.takeRequest();
    int numBlocksP2 = Math.toIntExact(toP2 - fromHeight + 1);
    assertBlockRequestParams(secondRequest, numBlocksP2, blockFilter, toP2, timeOption);
  }

  @ParameterizedTest(name = "[{index}] {0} empty blocks on 2nd page")
  @ValueSource(ints = {1, 2, 999, 1000})
  void getBlocksMultiplePagesNoEmptyFiltersRedundantBlocks(int numEmptyOnSecondPage)
      throws Exception {
    // Request the range [1000, 2999] spanning two full pages: [1000, 1999], [2000, 2999]
    long fromHeight = 1000;
    long toHeight = 2999;

    // First range is always full
    long startP1 = toHeight - MAX_BLOCKS_PER_REQUEST + 1;
    long toP2 = startP1 - 1;
    List<Block> page1Blocks = createBlocks(startP1, toHeight);
    // Second range contains some empty blocks:
    // In-range must be included in the response
    List<Block> page2InRangeBlocks = createBlocks(fromHeight, toP2 - numEmptyOnSecondPage);
    // Out-of-range must be discarded
    List<Block> page2OutOfRangeBlocks = createBlocks(fromHeight - numEmptyOnSecondPage,
        fromHeight - 1);
    List<Block> page2Blocks = concatLists(page2OutOfRangeBlocks, page2InRangeBlocks);
    // Self-check
    assertThat(page2Blocks, hasSize(MAX_BLOCKS_PER_REQUEST));
    assertThat(page2OutOfRangeBlocks, hasSize(numEmptyOnSecondPage));

    String firstResponse = createGetBlocksResponseWithEmpty(page1Blocks);
    String secondResponse = createGetBlocksResponseWithEmpty(page2Blocks);
    enqueueResponses(firstResponse, secondResponse);

    // Call
    BlockFilteringOption blockFilter = SKIP_EMPTY;
    BlockTimeOption timeOption = NO_COMMIT_TIME;
    List<Block> blocks = exonumClient.getBlocks(fromHeight, toHeight, blockFilter, timeOption);

    List<Block> expectedBlocks = concatLists(page2InRangeBlocks, page1Blocks);
    assertThat(blocks, equalTo(expectedBlocks));

    // Check the requests made
    RecordedRequest firstRequest = server.takeRequest();
    assertBlockRequestParams(firstRequest, MAX_BLOCKS_PER_REQUEST, blockFilter, toHeight,
        timeOption);

    RecordedRequest secondRequest = server.takeRequest();
    assertBlockRequestParams(secondRequest, MAX_BLOCKS_PER_REQUEST, blockFilter, toP2, timeOption);
  }

  @ParameterizedTest
  @CsvSource({
      "-1, 1, 'negative from'",
      "1, -1, 'negative to'",
      "-2, -1, 'negative from & to'",
      "2, 1, 'from > to'",
  })
  void getBlocksWrongRange(long fromHeight, long toHeight) {
    assertThrows(IllegalArgumentException.class,
        () -> exonumClient.getBlocks(fromHeight, toHeight, INCLUDE_EMPTY, NO_COMMIT_TIME));
  }

  @ParameterizedTest
  @ValueSource(ints = {Integer.MIN_VALUE, -1, 0})
  void getLastBlocksWrongBlocksCount(int blocksCount) {
    assertThrows(IllegalArgumentException.class,
        () -> exonumClient.getLastBlocks(blocksCount, INCLUDE_EMPTY, NO_COMMIT_TIME));
  }

  @Test
  void getLastBlocksSkippingEmptySinglePage() throws InterruptedException {
    int numBlocks = 95;
    long blockchainHeight = 99;

    long end = blockchainHeight + 1;
    long start = end - numBlocks;
    String mockResponse = "{\n"
        + "    'range': {\n"
        + "        'start': " + start + ",\n"
        + "        'end': " + end + "\n"
        + "    },\n"
        + "    'blocks': [ " + BLOCK_3_JSON + "," + BLOCK_2_JSON + "," + BLOCK_1_JSON + "],\n"
        + "    'times': ['" + BLOCK_3_TIME + "','" + BLOCK_2_TIME + "','" + BLOCK_1_TIME + "']\n"
        + "}\n";
    enqueueResponse(mockResponse);

    // Call
    BlockFilteringOption blockFilter = SKIP_EMPTY;
    BlockTimeOption timeOption = INCLUDE_COMMIT_TIME;
    BlocksRange response = exonumClient.getLastBlocks(numBlocks, blockFilter, timeOption);

    // Assert response
    BlocksRange expected = new BlocksRange(start, blockchainHeight,
        ImmutableList.of(BLOCK_1, BLOCK_2, BLOCK_3));
    assertThat(response, equalTo(expected));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertPath(recordedRequest, BLOCKS);
    assertBlockRequestParams(recordedRequest, numBlocks, blockFilter, null, timeOption);
  }

  @ParameterizedTest(name = "[{index}] {0} more than in the blockchain")
  @DisplayName("getLastBlocks requests more blocks than has been committed")
  @ValueSource(ints = {0, 1, 2, MAX_BLOCKS_PER_REQUEST - 1, MAX_BLOCKS_PER_REQUEST})
  void getLastBlocksSkippingEmptyMoreThanCommitted(int overflow)
      throws InterruptedException {
    int blockchainHeight = 100;
    int end = blockchainHeight + 1;
    String mockResponse = "{\n"
        + "    'range': {\n"
        + "        'start': 0,\n"
        + "        'end': " + end + "\n"
        + "    },\n"
        + "    'blocks': [ " + BLOCK_3_JSON + "," + BLOCK_2_JSON + "," + BLOCK_1_JSON + "],\n"
        + "    'times': ['" + BLOCK_3_TIME + "','" + BLOCK_2_TIME + "','" + BLOCK_1_TIME + "']\n"
        + "}\n";
    enqueueResponse(mockResponse);

    // Call
    // Request more than the blockchain height
    int blocksInBlockchain = blockchainHeight + 1;
    int blocksCount = blocksInBlockchain + overflow;
    BlockFilteringOption blockFilter = SKIP_EMPTY;
    BlockTimeOption timeOption = INCLUDE_COMMIT_TIME;
    BlocksRange response = exonumClient.getLastBlocks(blocksCount, blockFilter, timeOption);

    // Assert response
    BlocksRange expected = new BlocksRange(0, blockchainHeight,
        ImmutableList.of(BLOCK_1, BLOCK_2, BLOCK_3));
    assertThat(response, equalTo(expected));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertPath(recordedRequest, BLOCKS);
    int expectedFirstRequestSize = min(blocksCount, MAX_BLOCKS_PER_REQUEST);
    assertBlockRequestParams(recordedRequest, expectedFirstRequestSize, blockFilter, null,
        timeOption);
  }

  @ParameterizedTest(name = "[{index}] {0} height, {1} blocks on the 2nd page. {2}")
  @CsvSource({
      "5000, 1, '1001 blocks requested'",
      "5000, 999, '1999 blocks requested'",
      "5000, 1000, '2K blocks requested'",
      "1000, 1, '1001 blocks requested = 1001 in the blockchain'",
      "1999, 1000, '2K blocks requested = 2K in the blockchain'",
      "1998, 1000, '2K blocks requested > 1999 in the blockchain'",
  })
  void getLastBlocksMultiplePagesWithEmpty(int blockchainHeight, int secondPageSize,
      @SuppressWarnings("unused") String description)
      throws Exception {
    int numBlocks = MAX_BLOCKS_PER_REQUEST + secondPageSize;
    long startP1 = blockchainHeight - MAX_BLOCKS_PER_REQUEST + 1;

    List<Block> blocksP1 = createBlocks(startP1, blockchainHeight);
    String responseP1 = createGetBlocksResponseWithEmpty(blocksP1);
    enqueueResponse(responseP1);

    long startP2 = Math.max(0, blockchainHeight - numBlocks + 1);
    long toP2 = startP1 - 1;
    List<Block> blocksP2 = createBlocks(startP2, toP2);
    String responseP2 = createGetBlocksResponseWithEmpty(blocksP2);
    enqueueResponse(responseP2);

    // Call
    BlockFilteringOption blockFilter = INCLUDE_EMPTY;
    BlockTimeOption timeOption = NO_COMMIT_TIME;
    BlocksRange response = exonumClient.getLastBlocks(numBlocks, blockFilter, timeOption);

    List<Block> expectedBlocks = concatLists(blocksP2, blocksP1);
    BlocksRange expected = new BlocksRange(startP2, blockchainHeight, expectedBlocks);
    assertThat(response, equalTo(expected));

    // Verify requests:
    // The first request shall naturally request the maximum number of blocks it is allowed to
    RecordedRequest request1 = server.takeRequest();
    assertBlockRequestParams(request1, MAX_BLOCKS_PER_REQUEST, blockFilter, null, timeOption);

    // The second request shall request the remaining blocks
    RecordedRequest request2 = server.takeRequest();
    assertBlockRequestParams(request2, secondPageSize, blockFilter, toP2, timeOption);
  }

  @Test
  void findNonEmptyBlocksSinglePage() throws InterruptedException {
    int numBlocks = 3;
    long blockchainHeight = 99;

    long end = blockchainHeight + 1;
    long start = BLOCK_1.getHeight();
    String mockResponse = "{\n"
        + "    'range': {\n"
        + "        'start': " + start + ",\n"
        + "        'end': " + end + "\n"
        + "    },\n"
        + "    'blocks': [ " + BLOCK_3_JSON + "," + BLOCK_2_JSON + "," + BLOCK_1_JSON + "],\n"
        + "    'times': ['" + BLOCK_3_TIME + "','" + BLOCK_2_TIME + "','" + BLOCK_1_TIME + "']\n"
        + "}\n";
    enqueueResponse(mockResponse);

    // Call
    BlockTimeOption timeOption = INCLUDE_COMMIT_TIME;
    List<Block> blocks = exonumClient.findNonEmptyBlocks(numBlocks, timeOption);

    // Check the response
    List<Block> expected = of(BLOCK_1, BLOCK_2, BLOCK_3);
    assertThat(blocks, equalTo(expected));

    // Verify the request
    RecordedRequest request = server.takeRequest();
    assertBlockRequestParams(request, numBlocks, SKIP_EMPTY, null, timeOption);
  }

  @ParameterizedTest
  @ValueSource(ints = {
      10,
      1010
  })
  void findNonEmptyBlocksSinglePageMoreThanInBlockchain(int numBlocks) throws InterruptedException {
    int numNonEmptyBlocks = 3;
    long blockchainHeight = 99;

    long end = blockchainHeight + 1;
    long start = 0;
    String mockResponse = "{\n"
        + "    'range': {\n"
        + "        'start': " + start + ",\n"
        + "        'end': " + end + "\n"
        + "    },\n"
        + "    'blocks': [ " + BLOCK_3_JSON + "," + BLOCK_2_JSON + "," + BLOCK_1_JSON + "],\n"
        + "    'times': ['" + BLOCK_3_TIME + "','" + BLOCK_2_TIME + "','" + BLOCK_1_TIME + "']\n"
        + "}\n";
    enqueueResponse(mockResponse);

    // Call
    BlockTimeOption timeOption = INCLUDE_COMMIT_TIME;
    List<Block> blocks = exonumClient.findNonEmptyBlocks(numBlocks, timeOption);

    // Check the response
    assertThat(blocks, hasSize(numNonEmptyBlocks));
    List<Block> expected = of(BLOCK_1, BLOCK_2, BLOCK_3);
    assertThat(blocks, equalTo(expected));

    // Verify the request
    RecordedRequest request = server.takeRequest();
    int requestedBlocks = min(numBlocks, MAX_BLOCKS_PER_REQUEST);
    assertBlockRequestParams(request, requestedBlocks, SKIP_EMPTY, null, timeOption);
  }

  @ParameterizedTest(name = "[{index}] {0} height, {1} blocks on the 2nd page. {2}")
  @CsvSource({
      "5000, 10, '1100 blocks requested'",
      "5000, 1000, '2000 blocks requested'",
      "1000, 1, '1001 blocks requested = 1001 in blockchain'",
  })
  void findNonEmptyBlocksMultiplePages(long blockchainHeight, int secondPageSize,
      @SuppressWarnings("unused") String description)
      throws InterruptedException {

    long toP1 = blockchainHeight;
    long fromP1 = toP1 - MAX_BLOCKS_PER_REQUEST + 1;
    List<Block> blocksP1 = createBlocks(fromP1, toP1);

    long toP2 = fromP1 - 1;
    long fromP2 = toP2 - secondPageSize + 1;
    List<Block> blocksP2 = createBlocks(fromP2, toP2);

    String response1 = createGetBlocksResponseWithEmpty(blocksP1);
    String response2 = createGetBlocksResponseWithEmpty(blocksP2);
    enqueueResponses(response1, response2);

    // Call
    int numBlocks = MAX_BLOCKS_PER_REQUEST + secondPageSize;
    BlockTimeOption timeOption = NO_COMMIT_TIME;
    List<Block> blocks = exonumClient.findNonEmptyBlocks(numBlocks, timeOption);

    // Check the response
    List<Block> expected = concatLists(blocksP2, blocksP1);
    assertThat(blocks, equalTo(expected));

    // Verify the requests
    RecordedRequest r1 = server.takeRequest();
    assertBlockRequestParams(r1, MAX_BLOCKS_PER_REQUEST, SKIP_EMPTY, null, timeOption);

    RecordedRequest r2 = server.takeRequest();
    assertBlockRequestParams(r2, secondPageSize, SKIP_EMPTY, toP2, timeOption);
  }

  /**
   * Returns a response to 'get_blocks' request **possibly with** empty blocks (i.e., 'start'
   * and 'end' will be inferred from the passed blocks).
   *
   * @param blocks a list of blocks in ascending order
   */
  private static String createGetBlocksResponseWithEmpty(List<Block> blocks) {
    checkArgument(!blocks.isEmpty());
    Block first = blocks.get(0);
    long start = first.getHeight();
    Block last = blocks.get(blocks.size() - 1);
    long end = last.getHeight() + 1;
    checkArgument(start < end);

    return createGetBlocksResponse(start, end, blocks);
  }

  @SuppressWarnings("UnstableApiUsage")
  private static String createGetBlocksResponse(long start, long end, List<Block> blocks) {
    // Self-check the blocks to be in ascending order by height
    assertTrue(isInStrictOrder(blocks, comparing(Block::getHeight)));

    List<GetBlockResponseBlock> responseBlocks = blocks.stream()
        .map(b -> toResponseBlock(b))
        .collect(toList());
    String blocksResponse = JSON.toJson(Lists.reverse(responseBlocks));
    return "{\n"
        + "    'range': {\n"
        + "        'start': " + start + ",\n"
        + "        'end': " + end + "\n"
        + "    },\n"
        + "    'blocks': " + blocksResponse + ",\n"
        + "    'times': null\n"
        + "}\n";
  }

  private static GetBlockResponseBlock toResponseBlock(Block b) {
    return new GetBlockResponseBlock(
        b.getProposerId(),
        b.getHeight(),
        b.getNumTransactions(),
        b.getPreviousBlockHash(),
        b.getTxRootHash(),
        b.getStateHash());
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
    enqueueResponse(mockResponse);

    // Call
    Block block = exonumClient.getLastBlock();

    // Assert response
    assertThat(block, is(BLOCK_1_WITHOUT_TIME));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertPath(recordedRequest, BLOCKS);
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
    enqueueResponse(mockResponse);

    // Call
    Optional<Block> block = exonumClient.getLastNonEmptyBlock();

    // Assert response
    assertTrue(block.isPresent());
    assertThat(block.get(), is(BLOCK_1_WITHOUT_TIME));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertPath(recordedRequest, BLOCKS);
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
    enqueueResponse(mockResponse);

    // Call
    Optional<Block> block = exonumClient.getLastNonEmptyBlock();

    // Assert response
    assertFalse(block.isPresent());

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertPath(recordedRequest, BLOCKS);
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
    enqueueResponse(json);

    // Call
    long actualHeight = exonumClient.getBlockchainHeight();

    // Assert response
    assertThat(actualHeight, is(height));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertPath(recordedRequest, BLOCKS);
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

  /** Enqueues JSON responses with the given body, in the order they are passed. */
  private void enqueueResponses(String... jsonResponses) {
    for (String r : jsonResponses) {
      enqueueResponse(r);
    }
  }

  /** Enqueues a JSON response with the given body. */
  private void enqueueResponse(String jsonResponse) {
    server.enqueue(new MockResponse()
        .setHeader(CONTENT_TYPE, "application/json")
        .setBody(jsonResponse));
  }

  /**
   * Creates a list of blocks in the closed range [from; to].
   */
  private static List<Block> createBlocks(long from, long to) {
    return LongStream.rangeClosed(from, to)
        .mapToObj(h -> aBlock()
            .height(h)
            .proposerId((int) (h % 5))
            .build()
        )
        .collect(toList());
  }

  private static <T> ImmutableList<T> concatLists(List<? extends T> l1, List<? extends T> l2) {
    return ImmutableList.copyOf(Iterables.concat(l1, l2));
  }
}

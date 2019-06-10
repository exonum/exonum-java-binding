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
import static com.exonum.client.ExonumApi.MAX_BLOCKS_PER_REQUEST;
import static com.exonum.client.ExonumUrls.BLOCK;
import static com.exonum.client.ExonumUrls.BLOCKS;
import static com.exonum.client.request.BlockFilteringOption.INCLUDE_EMPTY;
import static com.exonum.client.request.BlockFilteringOption.SKIP_EMPTY;
import static com.exonum.client.request.BlockTimeOption.INCLUDE_COMMIT_TIME;
import static com.exonum.client.request.BlockTimeOption.NO_COMMIT_TIME;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.of;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.json.JsonSerializer;
import com.exonum.client.ExplorerApiHelper.GetBlockResponseBlock;
import com.exonum.client.request.BlockFilteringOption;
import com.exonum.client.request.BlockTimeOption;
import com.exonum.client.response.Block;
import com.exonum.client.response.BlockResponse;
import com.exonum.client.response.BlocksRange;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@Execution(ExecutionMode.SAME_THREAD)
class ExonumHttpClientBlocksIntegrationTest {

  private static final Gson JSON = JsonSerializer.builder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .create();

  private static MockWebServer server;
  private static ExonumClient exonumClient;

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
    BlocksRange response = exonumClient.getBlocks(fromHeight, toHeight, blockFilter, timeOption);

    // Assert response
    BlocksRange expected = new BlocksRange(fromHeight, toHeight,
        of(BLOCK_1, BLOCK_2, BLOCK_3));
    assertThat(response, equalTo(expected));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(BLOCKS));
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
    Block block = aBlock()
        .height(1050)
        .build();
    String mockResponse = "{\n"
        + "    'range': {\n"
        + "        'start': " + start + ",\n"
        + "        'end': " + toHeight + 1 + "\n"
        + "    },\n"
        + "    'blocks': [ " + JSON.toJson(toResponseBlock(block)) + "],\n"
        + "    'times': null\n"
        + "}\n";
    enqueueResponse(mockResponse);

    // Call
    BlockFilteringOption blockFilter = SKIP_EMPTY;
    BlockTimeOption timeOption = NO_COMMIT_TIME;
    BlocksRange response = exonumClient.getBlocks(fromHeight, toHeight, blockFilter, timeOption);

    // Assert response
    BlocksRange expected = new BlocksRange(fromHeight, toHeight, ImmutableList.of(block));
    assertThat(response, equalTo(expected));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(BLOCKS));
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
    BlocksRange response = exonumClient.getBlocks(fromHeight, toHeight, blockFilter, timeOption);

    // Assert response
    BlocksRange expected = new BlocksRange(fromHeight, toHeight,
        of(BLOCK_1_WITHOUT_TIME, BLOCK_2_WITHOUT_TIME, BLOCK_3_WITHOUT_TIME));
    assertThat(response, equalTo(expected));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(BLOCKS));
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
    String firstResponse = "{\n"
        + "    'range': {\n"
        + "        'start': " + startP1 + ",\n"
        + "        'end': " + endP1 + "\n"
        + "    },\n"
        + "    'blocks': [ " + JSON.toJson(toResponseBlock(firstPageBlock)) + "],\n"
        + "    'times': null\n"
        + "}\n";
    enqueueResponse(firstResponse);

    Block secondPageBlock = aBlock()
        .height(102)
        .build();
    String secondResponse = "{\n"
        + "    'range': {\n"
        + "        'start': " + fromHeight + ",\n"
        + "        'end': " + startP1 + "\n"
        + "    },\n"
        + "    'blocks': [ " + JSON.toJson(toResponseBlock(secondPageBlock)) + "],\n"
        + "    'times': null\n"
        + "}\n";
    enqueueResponse(secondResponse);

    // Call
    BlockFilteringOption blockFilter = SKIP_EMPTY;
    BlockTimeOption timeOption = NO_COMMIT_TIME;
    BlocksRange response = exonumClient.getBlocks(fromHeight, toHeight, blockFilter, timeOption);

    // Check response
    BlocksRange expected = new BlocksRange(fromHeight, toHeight,
        ImmutableList.of(secondPageBlock, firstPageBlock));
    assertThat(response, equalTo(expected));

    // Check the requests made
    RecordedRequest firstRequest = server.takeRequest();
    assertBlockRequestParams(firstRequest, MAX_BLOCKS_PER_REQUEST, blockFilter, toHeight,
        timeOption);

    RecordedRequest secondRequest = server.takeRequest();
    assertBlockRequestParams(secondRequest, 101, blockFilter, (startP1 - 1),
        timeOption);
  }

  @Test
  void getBlocksMultiplePagesWithEmptyTwoFullPages() throws Exception {
    // Use two full (even) pages: [1000, 1999], [2000, 2999]
    long fromHeight = 1000;
    long toHeight = 2999;
    long startP1 = toHeight - MAX_BLOCKS_PER_REQUEST + 1;
    long toP2 = startP1 - 1;
    List<Block> page1Blocks = createBlocks(startP1, toHeight);
    List<Block> page2Blocks = createBlocks(fromHeight, toP2);
    String firstResponse = createGetBlocksResponseWithEmpty(page1Blocks);
    enqueueResponse(firstResponse);

    String secondResponse = createGetBlocksResponseWithEmpty(page2Blocks);
    enqueueResponse(secondResponse);

    // Call
    BlockFilteringOption blockFilter = INCLUDE_EMPTY;
    BlockTimeOption timeOption = NO_COMMIT_TIME;
    BlocksRange response = exonumClient.getBlocks(fromHeight, toHeight, blockFilter, timeOption);

    List<Block> expectedBlocks = concatLists(page2Blocks, page1Blocks);
    BlocksRange expected = new BlocksRange(fromHeight, toHeight, expectedBlocks);
    assertThat(response, equalTo(expected));

    // Check the requests made
    RecordedRequest firstRequest = server.takeRequest();
    assertBlockRequestParams(firstRequest, MAX_BLOCKS_PER_REQUEST, blockFilter, toHeight,
        timeOption);

    RecordedRequest secondRequest = server.takeRequest();
    assertBlockRequestParams(secondRequest, MAX_BLOCKS_PER_REQUEST, blockFilter, toP2,
        timeOption);
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
    String mockResponse = "{\n"
        + "    'range': {\n"
        + "        'start': 6,\n"
        + "        'end': 288\n"
        + "    },\n"
        + "    'blocks': [ " + BLOCK_3_JSON + "," + BLOCK_2_JSON + "," + BLOCK_1_JSON + "],\n"
        + "    'times': ['" + BLOCK_3_TIME + "','" + BLOCK_2_TIME + "','" + BLOCK_1_TIME + "']\n"
        + "}\n";
    enqueueResponse(mockResponse);

    // Call
    int blocksCount = MAX_BLOCKS_PER_REQUEST;
    BlockFilteringOption blockFilter = SKIP_EMPTY;
    BlockTimeOption timeOption = INCLUDE_COMMIT_TIME;
    BlocksRange response = exonumClient.getLastBlocks(blocksCount, blockFilter, timeOption);

    // Assert response
    BlocksRange expected = new BlocksRange(6L, 287L,
        ImmutableList.of(BLOCK_1, BLOCK_2, BLOCK_3));
    assertThat(response, equalTo(expected));

    // Assert request params
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod(), is("GET"));
    assertThat(recordedRequest.getPath(), startsWith(BLOCKS));
    assertBlockRequestParams(recordedRequest, blocksCount, blockFilter, null, timeOption);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, MAX_BLOCKS_PER_REQUEST - 1, MAX_BLOCKS_PER_REQUEST})
  void getLastBlocksMultiplePagesWithEmpty(int secondPageSize) throws Exception {
    int numBlocks = MAX_BLOCKS_PER_REQUEST + secondPageSize;
    long blockchainHeight = 5000;
    long startP1 = blockchainHeight - MAX_BLOCKS_PER_REQUEST + 1;

    List<Block> blocksP1 = createBlocks(startP1, blockchainHeight);
    String responseP1 = createGetBlocksResponseWithEmpty(blocksP1);
    enqueueResponse(responseP1);

    long startP2 = blockchainHeight - numBlocks + 1;
    long toP2 = startP1 - 1;
    List<Block> blocksP2 = createBlocks(startP2, toP2);
    String responseP2 = createGetBlocksResponseWithEmpty(blocksP2);
    enqueueResponse(responseP2);

    // Call
    BlockFilteringOption blockFilter = INCLUDE_EMPTY;
    BlockTimeOption timeOption = NO_COMMIT_TIME;
    BlocksRange response = exonumClient.getLastBlocks(numBlocks, blockFilter, timeOption);

    List<Block> expectedBlocks = concatLists(blocksP1, blocksP2);
    assertAll(
        () -> assertThat(response.getBlocks(), equalTo(expectedBlocks)),
        () -> assertThat(response.getFromHeight(), equalTo(startP2)),
        () -> assertThat(response.getToHeight(), equalTo(blockchainHeight))
    );

    // Verify requests:
    // The first request shall naturally request the maximum number of blocks it is allowed to
    RecordedRequest request1 = server.takeRequest();
    assertBlockRequestParams(request1, MAX_BLOCKS_PER_REQUEST, blockFilter, null, timeOption);

    // The second request shall get the one remaining
    RecordedRequest request2 = server.takeRequest();
    assertBlockRequestParams(request2, secondPageSize, blockFilter, toP2, timeOption);
  }

  /**
   * Returns a response to 'get_blocks' request **with** empty blocks (i.e., 'start' and 'end'
   * can be inferred from the passed blocks).
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
    enqueueResponse(mockResponse);

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
    enqueueResponse(mockResponse);

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
    enqueueResponse(json);

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

  /** Enqueues a JSON response with the given body. */
  private static void enqueueResponse(String jsonResponse) {
    server.enqueue(new MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody(jsonResponse));
  }

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

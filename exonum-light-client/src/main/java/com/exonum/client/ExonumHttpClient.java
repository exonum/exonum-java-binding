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

import static com.exonum.client.ExonumApi.MAX_BLOCKS_PER_REQUEST;
import static com.exonum.client.ExonumIterables.indexOf;
import static com.exonum.client.ExonumUrls.BLOCK;
import static com.exonum.client.ExonumUrls.BLOCKS;
import static com.exonum.client.ExonumUrls.HEALTH_CHECK;
import static com.exonum.client.ExonumUrls.MEMORY_POOL;
import static com.exonum.client.ExonumUrls.TRANSACTIONS;
import static com.exonum.client.ExonumUrls.USER_AGENT;
import static com.exonum.client.request.BlockFilteringOption.INCLUDE_EMPTY;
import static com.exonum.client.request.BlockFilteringOption.SKIP_EMPTY;
import static com.exonum.client.request.BlockTimeOption.INCLUDE_COMMIT_TIME;
import static com.exonum.client.request.BlockTimeOption.NO_COMMIT_TIME;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.client.request.BlockFilteringOption;
import com.exonum.client.request.BlockTimeOption;
import com.exonum.client.response.Block;
import com.exonum.client.response.BlockResponse;
import com.exonum.client.response.BlocksRange;
import com.exonum.client.response.BlocksResponse;
import com.exonum.client.response.HealthCheckInfo;
import com.exonum.client.response.TransactionResponse;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Implementation of the {@linkplain ExonumClient} which works over HTTP REST API.
 * It uses {@linkplain OkHttpClient} internally for REST API communication with Exonum node.
 */
class ExonumHttpClient implements ExonumClient {
  private static final MediaType MEDIA_TYPE_JSON = MediaType.get("application/json; charset=utf-8");
  private static final int GENESIS_BLOCK_HEIGHT = 0;

  private final OkHttpClient httpClient;
  private final URL exonumHost;

  ExonumHttpClient(OkHttpClient httpClient, URL exonumHost) {
    this.httpClient = httpClient;
    this.exonumHost = exonumHost;
  }

  @Override
  public HashCode submitTransaction(TransactionMessage transactionMessage) {
    Request request = post(toFullUrl(TRANSACTIONS),
        ExplorerApiHelper.createSubmitTxBody(transactionMessage));

    return blockingExecuteAndParse(request, ExplorerApiHelper::parseSubmitTxResponse);
  }

  @Override
  public int getUnconfirmedTransactionsCount() {
    Request request = get(toFullUrl(MEMORY_POOL));

    return blockingExecuteAndParse(request, SystemApiHelper::parseMemoryPoolJson);
  }

  @Override
  public HealthCheckInfo healthCheck() {
    Request request = get(toFullUrl(HEALTH_CHECK));

    return blockingExecuteAndParse(request, SystemApiHelper::parseHealthCheckJson);
  }

  @Override
  public String getUserAgentInfo() {
    Request request = get(toFullUrl(USER_AGENT));

    return blockingExecutePlainText(request);
  }

  @Override
  public Optional<TransactionResponse> getTransaction(HashCode id) {
    HashCode hash = checkNotNull(id);
    HttpUrl url = urlBuilder()
        .encodedPath(TRANSACTIONS)
        .addQueryParameter("hash", hash.toString())
        .build();
    Request request = get(url);

    return blockingExecute(request, response -> {
      if (response.code() == HTTP_NOT_FOUND) {
        return Optional.empty();
      } else if (!response.isSuccessful()) {
        throw new RuntimeException("Execution wasn't successful: " + response.toString());
      } else {
        TransactionResponse txResponse = ExplorerApiHelper
            .parseGetTxResponse(readBody(response));

        return Optional.of(txResponse);
      }
    });
  }

  @Override
  public long getBlockchainHeight() {
    BlocksResponse response = doGetBlocks(0, INCLUDE_EMPTY, null, NO_COMMIT_TIME);

    return response.getBlocksRangeEnd() - 1; // Because '$.range.end' is exclusive
  }

  @Override
  public BlockResponse getBlockByHeight(long height) {
    checkArgument(0 <= height, "Height can't be negative, but was %s", height);
    HttpUrl url = urlBuilder()
        .encodedPath(BLOCK)
        .addQueryParameter("height", String.valueOf(height))
        .build();
    Request request = get(url);

    return blockingExecuteAndParse(request, ExplorerApiHelper::parseGetBlockResponse);
  }

  @Override
  public List<Block> getBlocks(long fromHeight, long toHeight, BlockFilteringOption blockFilter,
      BlockTimeOption timeOption) {
    checkArgument(0 <= fromHeight, "First block height (%s) must be non-negative", fromHeight);
    checkArgument(fromHeight <= toHeight,
        "First block height (%s) should be less than or equal to the last block height (%s)",
        fromHeight, toHeight);

    // 'maximum' as when skipping empty the actual might be way smaller
    int maxSize = Math.toIntExact(toHeight - fromHeight + 1);
    List<Block> blocks = new ArrayList<>(maxSize);
    for (long rangeLast = toHeight; rangeLast >= fromHeight; ) {
      int remainingBlocks = Math.toIntExact(rangeLast - fromHeight + 1);
      int numBlocks = min(remainingBlocks, MAX_BLOCKS_PER_REQUEST);
      BlocksResponse blocksResponse = doGetBlocks(numBlocks, blockFilter, rangeLast, timeOption);

      blocks.addAll(blocksResponse.getBlocks());

      rangeLast = blocksResponse.getBlocksRangeStart() - 1;
    }

    return postProcessResponseBlocks(fromHeight, toHeight, blocks)
        .getBlocks();
  }

  @Override
  public BlocksRange getLastBlocks(int count, BlockFilteringOption blockFilter,
      BlockTimeOption timeOption) {
    checkArgument(0 < count,
        "Requested number of blocks should be positive but was %s", count);

    List<Block> blocks = new ArrayList<>(count);
    // The first request does not specify the maximum height to get the top blocks
    long blockchainHeight = Long.MIN_VALUE;
    Long nextHeight = null;
    int remainingBlocks = count;
    while (remainingBlocks > 0
        && (nextHeight == null || nextHeight >= GENESIS_BLOCK_HEIGHT)) {
      int numBlocks = min(remainingBlocks, MAX_BLOCKS_PER_REQUEST);
      BlocksResponse blocksResponse = doGetBlocks(numBlocks, blockFilter, nextHeight, timeOption);

      blocks.addAll(blocksResponse.getBlocks());

      nextHeight = blocksResponse.getBlocksRangeStart() - 1;
      blockchainHeight = max(blockchainHeight, blocksResponse.getBlocksRangeEnd() - 1);
      remainingBlocks = Math.toIntExact(count - (blockchainHeight - nextHeight));
    }

    long fromHeight = max(blockchainHeight - count + 1, GENESIS_BLOCK_HEIGHT);
    long toHeight = blockchainHeight;
    return postProcessResponseBlocks(fromHeight, toHeight, blocks);
  }

  /**
   * Post-processes the blocks, coming from
   * {@link #doGetBlocks(int, BlockFilteringOption, Long, BlockTimeOption)}:
   * 1. Turns them in ascending order by height.
   * 2. Keeps only blocks that fall in range [fromHeight; toHeight].
   */
  private static BlocksRange postProcessResponseBlocks(long fromHeight, long toHeight,
      List<Block> blocks) {
    // Turn the blocks in ascending order
    blocks = Lists.reverse(blocks);

    // Filter the possible blocks that are out of the requested range
    // No Stream#dropWhile in Java 8 :(
    int firstInRange = indexOf(blocks, b -> b.getHeight() >= fromHeight)
        .orElse(blocks.size());
    blocks = blocks.subList(firstInRange, blocks.size());

    // Do not bother trimming — BlocksRange copies the list
    return new BlocksRange(fromHeight, toHeight, blocks);
  }

  @Override
  public List<Block> findNonEmptyBlocks(int numBlocks, BlockTimeOption timeOption) {
    checkArgument(0 < numBlocks,
        "Requested number of blocks should be positive but was %s", numBlocks);

    List<Block> blocks = new ArrayList<>(numBlocks);
    Long nextHeight = null;
    int remainingBlocks = numBlocks;
    while (remainingBlocks > 0
        && (nextHeight == null || nextHeight >= GENESIS_BLOCK_HEIGHT)) {
      int numRequested = min(remainingBlocks, MAX_BLOCKS_PER_REQUEST);
      BlocksResponse blocksResponse = doGetBlocks(numRequested, SKIP_EMPTY, nextHeight, timeOption);

      blocks.addAll(blocksResponse.getBlocks());

      nextHeight = blocksResponse.getBlocksRangeStart() - 1;
      remainingBlocks -= blocksResponse.getBlocks().size();
    }

    List<Block> ascBlocks = Lists.reverse(blocks);
    return ImmutableList.copyOf(ascBlocks);
  }

  @Override
  public Block getLastBlock() {
    BlocksResponse response = doGetBlocks(1, INCLUDE_EMPTY, null, INCLUDE_COMMIT_TIME);

    return response.getBlocks()
        .stream()
        .findFirst()
        .orElseThrow(() -> new AssertionError("Should never happen, response: " + response));
  }

  @Override
  public Optional<Block> getLastNonEmptyBlock() {
    BlocksResponse response = doGetBlocks(1, SKIP_EMPTY, null, INCLUDE_COMMIT_TIME);

    return response.getBlocks()
        .stream()
        .findFirst();
  }

  private BlocksResponse doGetBlocks(int count, BlockFilteringOption blockFilter, Long heightMax,
      BlockTimeOption timeOption) {
    checkArgument(count <= MAX_BLOCKS_PER_REQUEST,
        "Requested number of blocks was %s but maximum allowed is %s",
        count, MAX_BLOCKS_PER_REQUEST);
    checkArgument(heightMax == null || 0 <= heightMax,
        "Blockchain height can't be negative but was %s", heightMax);

    boolean skipEmpty = blockFilter == SKIP_EMPTY;
    boolean withTime = timeOption == INCLUDE_COMMIT_TIME;
    Map<String, String> query = new HashMap<>();
    query.put("count", String.valueOf(count));
    query.put("skip_empty_blocks", String.valueOf(skipEmpty));
    query.put("add_blocks_time", String.valueOf(withTime));
    if (heightMax != null) {
      query.put("latest", String.valueOf(heightMax));
    }

    HttpUrl.Builder httpRequest = urlBuilder().encodedPath(BLOCKS);
    query.forEach(httpRequest::addQueryParameter);

    Request request = get(httpRequest.build());

    return blockingExecuteAndParse(request, ExplorerApiHelper::parseGetBlocksResponse);
  }

  private static Request get(HttpUrl url) {
    return new Request.Builder()
        .url(url)
        .get()
        .build();
  }

  private static Request post(HttpUrl url, String jsonBody) {
    return new Request.Builder()
        .url(url)
        .post(RequestBody.create(MEDIA_TYPE_JSON, jsonBody))
        .build();
  }

  private HttpUrl toFullUrl(String relativeUrl) {
    return urlBuilder()
        .encodedPath(relativeUrl)
        .build();
  }

  private HttpUrl.Builder urlBuilder() {

    return new HttpUrl.Builder()
        .scheme(exonumHost.getProtocol())
        .host(exonumHost.getHost())
        .port(exonumHost.getPort());
  }

  private <T> T blockingExecute(Request request, Function<Response, T> responseHandler) {
    try (Response response = httpClient.newCall(request).execute()) {
      return responseHandler.apply(response);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String blockingExecutePlainText(Request request) {
    return blockingExecute(request, response -> {
      if (!response.isSuccessful()) {
        throw new RuntimeException("Execution wasn't successful: " + response.toString());
      }
      return readBody(response);
    });
  }

  private <T> T blockingExecuteAndParse(Request request, Function<String, T> parser) {
    String response = blockingExecutePlainText(request);
    return parser.apply(response);
  }

  private static String readBody(Response response) {
    try {
      return response.body().string();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}

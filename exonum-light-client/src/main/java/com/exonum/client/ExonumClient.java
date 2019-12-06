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

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.client.request.BlockFilteringOption;
import com.exonum.client.request.BlockTimeOption;
import com.exonum.client.response.Block;
import com.exonum.client.response.BlockResponse;
import com.exonum.client.response.BlocksRange;
import com.exonum.client.response.HealthCheckInfo;
import com.exonum.client.response.TransactionResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import okhttp3.OkHttpClient;

/**
 * Main interface for Exonum Light client.
 * Provides a convenient way for interaction with Exonum framework APIs.
 * All the methods of the interface work in a blocking way,
 * i.e., invoke underlying request immediately, and block until the response can be processed
 * or an error occurs. In case the thread is interrupted, the blocked methods will complete
 * exceptionally.
 *
 * <p><em>Implementations of this interface are required to be thread-safe</em>.
 **/
public interface ExonumClient {

  /**
   * Submits the transaction message to an Exonum node.
   * @return transaction message hash
   * @throws RuntimeException if the client is unable to submit the transaction
   *        (e.g., in case of connectivity problems)
   */
  HashCode submitTransaction(TransactionMessage tx);

  /**
   * Returns a number of unconfirmed transactions which are currently located in
   * the unconfirmed transactions pool and are waiting for acceptance to a block.
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   */
  int getUnconfirmedTransactionsCount();

  /**
   * Returns the node health check information.
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   */
  HealthCheckInfo healthCheck();

  /**
   * Returns string containing information about Exonum, Rust and OS version.
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   */
  String getUserAgentInfo();

  /**
   * Returns the information about the transaction; or {@code Optional.empty()}
   * if the requested transaction is not found.
   * @param id transaction message hash
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   */
  Optional<TransactionResponse> getTransaction(HashCode id);

  /**
   * Returns the <em>blockchain height</em> which is the height of the latest committed block
   * in the blockchain. The block height is a distance between the last block
   * and the "genesis", or initial, block. Therefore, the blockchain height is equal to the number
   * of blocks plus one.
   *
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   */
  long getBlockchainHeight();

  /**
   * Returns the information about the block with transaction hashes included at this block.
   * @param height blockchain height starting from 0 (genesis block)
   * @return block information response
   * @throws IllegalArgumentException if the given height is negative; or greater than
   *        the actual blockchain height
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   */
  BlockResponse getBlockByHeight(long height);

  /**
   * Returns blockchain blocks in the requested <em>closed</em> range. The blocks are returned
   * in ascending order by their height.
   *
   * @param fromHeight the height of the first block to include. Must be non-negative
   * @param toHeight the height of the last block to include. Must be greater than
   *        or equal to {@code fromHeight} and less than or equal to the blockchain height.
   * @param blockFilter controls whether to skip blocks with no transactions
   * @param timeOption controls whether to include
   *        the {@linkplain Block#getCommitTime() block commit time}
   * @return blocks in the requested range
   * @throws IllegalArgumentException if {@code fromHeight} or {@code toHeight} are not valid:
   *        out of range {@code [0, blockchainHeight]}; {@code fromHeight} > {@code toHeight}
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   */
  List<Block> getBlocks(long fromHeight, long toHeight, BlockFilteringOption blockFilter,
      BlockTimeOption timeOption);

  /**
   * Returns the range of the most recent blockchain blocks in ascending order by their height.
   * More precisely, returns the blocks in the closed range
   * {@code [max(0, blockchainHeight - size + 1), blockchainHeight]} of size
   * {@code max(blockchainHeight + 1, size)}.
   *
   * @param size the size of the range. If it exceeds the number of blocks in the blockchain,
   *        this method will return all blocks ({@code blockchainHeight + 1})
   * @param blockFilter controls whether to skip blocks with no transactions. If filtering
   *        is applied, the actual number of blocks may be smaller than {@code size};
   *        but the range of blocks will not be extended beyond {@code blockchainHeight - size + 1}.
   *        If a certain <em>number</em> of non-empty blocks is needed (not a certain
   *        <em>range</em>), use {@link #findNonEmptyBlocks(int, BlockTimeOption)}
   * @param timeOption controls whether to include
   *        the {@linkplain Block#getCommitTime() block commit time}
   * @return blocks information response
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   * @throws IllegalArgumentException if size is non-positive
   * @see #findNonEmptyBlocks(int, BlockTimeOption)
   */
  BlocksRange getLastBlocks(int size, BlockFilteringOption blockFilter,
      BlockTimeOption timeOption);

  /**
   * Returns up to the given number of the most recent non-empty blocks in ascending order
   * by their height. The search range is not limited, i.e., spans the whole blockchain.
   *
   * @param numBlocks the maximum number of blocks to return. Must be positive. If the number
   *     of non-empty blocks in the blockchain is less than {@code numBlocks}, all such blocks
   *     will be returned
   * @param timeOption controls whether to include
   *        the {@linkplain Block#getCommitTime() block commit time}
   * @return a list of the most recent non-empty blocks
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   * @throws IllegalArgumentException if numBlocks is non-positive
   * @see #getLastBlocks(int, BlockFilteringOption, BlockTimeOption)
   * @see BlockFilteringOption#SKIP_EMPTY
   */
  List<Block> findNonEmptyBlocks(int numBlocks, BlockTimeOption timeOption);

  /**
   * Returns the last block in the blockchain.
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   */
  Block getLastBlock();

  /**
   * Returns the last block in the blockchain which contains transactions;
   * or {@code Optional.empty()} if there are no blocks with transactions in the blockchain.
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   */
  Optional<Block> getLastNonEmptyBlock();

  /**
   * Returns Exonum client builder.
   */
  static Builder newBuilder() {
    return new Builder();
  }

  /**
   * Builder class for the Exonum client.
   */
  class Builder {
    private static final OkHttpClient DEFAULT_CLIENT = new OkHttpClient();

    private URL exonumHost;
    private OkHttpClient httpClient = DEFAULT_CLIENT;
    private String prefix = "";

    /**
     * Sets Exonum host url.
     */
    public Builder setExonumHost(URL exonumHost) {
      this.exonumHost = checkNotNull(exonumHost);
      return this;
    }

    /**
     * Sets Exonum host url.
     * @throws IllegalArgumentException if the url is malformed
     */
    public Builder setExonumHost(String exonumHost) {
      String host = checkNotNull(exonumHost);
      try {
        return setExonumHost(new URL(host));
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException(e);
      }
    }

    /**
     * Sets http client, optional. If not set a default instance of http client will be used.
     *
     * <p>This method provides a flexibility for the Exonum client configuration.
     * Can be helpful, for example, in case a network proxy configuration is needed
     * or <a href="https://github.com/square/okhttp/tree/master/okhttp-logging-interceptor">
     *   request/response logging.</a>
     */
    public Builder setHttpClient(OkHttpClient client) {
      this.httpClient = checkNotNull(client);
      return this;
    }

    /**
     * Sets an optional URL prefix to be applied to all requests made by the client.
     * Can be helpful in case of using middleware routing proxy on the blockchain node side.
     * There is no prefix by default.
     */
    public Builder setPrefix(String prefix) {
      this.prefix = checkNotNull(prefix);
      return this;
    }

    /**
     * Creates Exonum client instance.
     * @throws IllegalStateException if required fields weren't set
     */
    public ExonumClient build() {
      checkRequiredFieldsSet();
      return new ExonumHttpClient(httpClient, exonumHost, prefix);
    }

    private void checkRequiredFieldsSet() {
      String undefinedFields = "";
      undefinedFields = exonumHost == null ? undefinedFields + " exonumHost" : undefinedFields;
      if (!undefinedFields.isEmpty()) {
        throw new IllegalStateException(
            "Following field(s) are required but weren't set: " + undefinedFields);
      }
    }
  }

}

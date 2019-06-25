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
 * All the methods of the interface work in a blocking way
 * i.e. invoke underlying request immediately, and block until the response can be processed
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
   * @throws RuntimeException if block is not found by the requested height,
   *        i.e. the requested height is greater than actual blockchain height
   * @throws IllegalArgumentException if the given height is negative
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
   *        If the {@code toHeight} is greater than actual blockchain height then
   *        the actual height will be used — such error-prone behaviour will be fixed
   *        in Exonum 0.12, see ECR-3188)
   * @param blockFilter controls whether to skip blocks with no transactions
   * @param timeOption controls whether to include the block commit time.
   *        See {@linkplain Block#getCommitTime()}.
   *        The time value corresponds to the average time of submission of precommits by the
   *        validators for every returned block
   * @return blocks in the requested range
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   * @throws IllegalArgumentException if {@code fromHeight} or {@code toHeight} are not valid
   */
  List<Block> getBlocks(long fromHeight, long toHeight, BlockFilteringOption blockFilter,
      BlockTimeOption timeOption);

  /**
   * Returns the given number of the most recent blockchain blocks in ascending order
   * by their height. More precisely, returns blocks in the range
   * {@code [max(0, blockchainHeight - numBlocks + 1), blockchainHeight]}.
   *
   * @param numBlocks Number of blocks to return. If the number of blocks in the blockchain is less
   *        than {@code numBlocks}, this method will return all blocks
   * @param blockFilter controls whether to skip blocks with no transactions. If filtering
   *        is applied, the actual number of blocks may be smaller than {@code numBlocks};
   *        but the range of blocks will not be extended
   * @param timeOption controls whether to include the block commit time.
   *        See {@linkplain Block#getCommitTime()}.
   *        The time value corresponds to the average time of submission of precommits by the
   *        validators for every returned block
   * @return blocks information response
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   * @throws IllegalArgumentException if numBlocks is non-positive
   */
  BlocksRange getLastBlocks(int numBlocks, BlockFilteringOption blockFilter,
      BlockTimeOption timeOption);

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
     * or request/response logging.
     */
    public Builder setHttpClient(OkHttpClient client) {
      this.httpClient = checkNotNull(client);
      return this;
    }

    /**
     * Creates Exonum client instance.
     * @throws IllegalStateException if required fields weren't set
     */
    public ExonumClient build() {
      checkRequiredFieldsSet();
      return new ExonumHttpClient(httpClient, exonumHost);
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

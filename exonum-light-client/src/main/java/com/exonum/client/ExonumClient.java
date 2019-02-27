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
import com.exonum.client.response.Block;
import com.exonum.client.response.BlockResponse;
import com.exonum.client.response.BlocksResponse;
import com.exonum.client.response.HealthCheckInfo;
import com.exonum.client.response.TransactionResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import okhttp3.OkHttpClient;

/**
 * Main interface for Exonum Light client.
 * Provides a convenient way for interaction with Exonum framework APIs.
 * All the methods of the interface work in a blocking way
 * i.e. invoke underlying request immediately, and block until the response can be processed
 * or an error occurs.
 * <p/><i>Implementations of that interface are required to be thread-safe</i>.
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
   * <p>For example, the "genesis" block has height {@code h = 0}. The latest committed block
   * has height {@code h = getBlockHashes().size() - 1}.
   *
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   */
  long getBlockchainHeight();

  /**
   * Returns the information about the block with transaction hashes included at this block.
   * @param height blockchain height starting from 0 (genesis block)
   * @return block information response
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   * @throws IllegalArgumentException if the given height is negative
   */
  BlockResponse getBlockByHeight(long height);

  /**
   * Returns blockchain blocks information for the requested range.
   * @param count Number of blocks to return.
   *        Should not be greater then {@linkplain ExonumApi#MAX_BLOCKS_PER_REQUEST}
   * @param skipEmpty if {@code true}, then only non-empty blocks will be returned
   * @param heightMax maximum height of the returned blocks. The blocks are returned
   *        in reverse order, starting from the <b>heightMax</b> and
   *        at least up to the <b>heightMax - count + 1</b>.
   *        If the <b>heightMax</b> is greater than actual blockchain height then
   *        the actual height will be used
   * @param withBlocksTime if {@code true}, then includes block commit times in the response;
   *        or an empty times {@code false}. See {@linkplain BlocksResponse#getBlockCommitTimes()}
   *        The time value corresponds to the average time of submission of precommits by the
   *        validators for every returned block
   * @return blocks information response
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   * @throws IllegalArgumentException if count is greater
   *        then {@linkplain ExonumApi#MAX_BLOCKS_PER_REQUEST}
   */
  BlocksResponse getBlocks(int count, boolean skipEmpty, long heightMax, boolean withBlocksTime);

  /**
   * Returns blockchain blocks information starting from the last block in the blockchain.
   * @param count Number of blocks to return.
   *        Should not be greater than {@linkplain ExonumApi#MAX_BLOCKS_PER_REQUEST}
   * @param skipEmpty if {@code true}, then only non-empty blocks are returned
   * @param withBlocksTime if {@code true}, then returns an array of
   *        {@linkplain java.time.ZonedDateTime} objects; or an empty array if {@code false}.
   *        The time value corresponds to the average time of submission of precommits by the
   *        validators for every returned block
   * @return blocks information response
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   */
  BlocksResponse getLastBlocks(int count, boolean skipEmpty, boolean withBlocksTime);

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
     * <p/>This method provides a flexibility for the Exonum client configuration.
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

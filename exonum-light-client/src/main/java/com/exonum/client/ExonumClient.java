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
import java.net.MalformedURLException;
import java.net.URL;
import okhttp3.OkHttpClient;

/**
 * Main interface for Exonum Light client.
 * Provides a convenient way for interaction with Exonum framework APIs.
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
   * Returns a number of unconfirmed transactions those are currently located in
   * the memory pool and are waiting for acceptance to a block.
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   */
  int getUnconfirmedTransactions();

  /**
   * Returns <b>true</b> if the node is connected to the other peers.
   * And <b>false</b> otherwise.
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   */
  boolean healthCheck();

  /**
   * Returns string containing information about Exonum, Rust and OS version.
   * @throws RuntimeException if the client is unable to complete a request
   *        (e.g., in case of connectivity problems)
   */
  String getUserAgentInfo();

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

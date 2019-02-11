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

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import java.net.MalformedURLException;
import java.net.URL;
import okhttp3.OkHttpClient;

/**
 * Main class to start using Exonum Light client.
 * Provides a convenient way for interaction with Exonum framework APIs.
 * It uses {@linkplain OkHttpClient} internally for REST API communication with Exonum node.
 **/
public interface ExonumClient {

  /**
   * Submits the transaction message to an Exonum node.
   * @return transaction message hash
   * @throws RuntimeException if network request fails
   */
  HashCode submitTransaction(TransactionMessage tx);

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
      this.exonumHost = exonumHost;
      return this;
    }

    /**
     * Sets Exonum host url.
     * @throws IllegalArgumentException if the url is malformed
     */
    public Builder setExonumHost(String exonumHost) {
      try {
        return setExonumHost(new URL(exonumHost));
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
      this.httpClient = client;
      return this;
    }

    /**
     * Creates Exonum client instance.
     * @throws IllegalStateException if required fields weren't set
     */
    public ExonumClient build() {
      checkRequiredFieldsSet();
      return new ExonumClientImpl(httpClient, exonumHost);
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

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

import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static com.exonum.client.ExonumUrls.SUBMIT_TRANSACTION;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.google.common.io.BaseEncoding;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class ExonumClientImpl implements ExonumClient {
  private static final BaseEncoding HEX_ENCODER = BaseEncoding.base16().lowerCase();
  private static final MediaType MEDIA_TYPE_JSON = MediaType.get("application/json; charset=utf-8");

  private final OkHttpClient httpClient;
  private final URL exonumHost;

  private URL submitTxUrl;

  ExonumClientImpl(OkHttpClient httpClient, URL exonumHost) {
    this.httpClient = httpClient;
    this.exonumHost = exonumHost;
    createUrls();
  }

  @Override
  public HashCode submitTransaction(TransactionMessage transactionMessage) {
    String msg = toHex(transactionMessage.toBytes());

    Request request = createRequest(submitTxUrl, new SubmitTxRequest(msg));
    SubmitTxResponse result = blockingExecuteWithResponse(request, SubmitTxResponse.class);

    return result.hash;
  }

  private static String toHex(byte[] array) {
    return HEX_ENCODER.encode(array);
  }

  private static Request createRequest(URL url, Object requestBody) {
    String jsonBody = json().toJson(requestBody);

    return new Request.Builder()
        .url(url)
        .post(RequestBody.create(MEDIA_TYPE_JSON, jsonBody))
        .build();
  }

  private <T> T blockingExecuteWithResponse(Request request, Class<T> responseObject) {
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new RuntimeException("Execution wasn't success: " + response.toString());
      }
      String responseJson = response.body().string();

      return json().fromJson(responseJson, responseObject);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void createUrls() {
    try {
      this.submitTxUrl = new URL(exonumHost, SUBMIT_TRANSACTION);
    } catch (MalformedURLException e) {
      throw new AssertionError("Should never happen", e);
    }
  }

}

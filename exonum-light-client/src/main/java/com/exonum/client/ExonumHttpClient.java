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
import static com.exonum.client.ExonumUrls.HEALTH_CHECK;
import static com.exonum.client.ExonumUrls.MEMORY_POOL;
import static com.exonum.client.ExonumUrls.SUBMIT_TRANSACTION;
import static com.exonum.client.ExonumUrls.USER_AGENT;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.client.ExplorerApiHelper.SubmitTxRequest;
import com.exonum.client.response.HealthCheckInfo;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Function;
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
  private final OkHttpClient httpClient;
  private final URL exonumHost;

  ExonumHttpClient(OkHttpClient httpClient, URL exonumHost) {
    this.httpClient = httpClient;
    this.exonumHost = exonumHost;
  }

  @Override
  public HashCode submitTransaction(TransactionMessage transactionMessage) {
    Request request = postRequest(toFullUrl(SUBMIT_TRANSACTION),
        new SubmitTxRequest(transactionMessage));

    return blockingExecuteAndParse(request, ExplorerApiHelper::submitTxParser);
  }

  @Override
  public int getUnconfirmedTransactionsCount() {
    Request request = getRequest(toFullUrl(MEMORY_POOL));

    return blockingExecuteAndParse(request, SystemApiHelper::memoryPoolJsonParser);
  }

  @Override
  public HealthCheckInfo healthCheck() {
    Request request = getRequest(toFullUrl(HEALTH_CHECK));

    return blockingExecuteAndParse(request, SystemApiHelper::healthCheckJsonParser);
  }

  @Override
  public String getUserAgentInfo() {
    Request request = getRequest(toFullUrl(USER_AGENT));

    return blockingExecutePlainText(request);
  }

  private static Request getRequest(URL url) {
    return new Request.Builder()
        .url(url)
        .get()
        .build();
  }

  private static Request postRequest(URL url, Object body) {
    String jsonBody = json().toJson(body);

    return new Request.Builder()
        .url(url)
        .post(RequestBody.create(MEDIA_TYPE_JSON, jsonBody))
        .build();
  }

  private URL toFullUrl(String relativeUrl) {
    try {
      return new URL(exonumHost, relativeUrl);
    } catch (MalformedURLException e) {
      throw new AssertionError("Should never happen", e);
    }
  }

  private String blockingExecutePlainText(Request request) {
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new RuntimeException("Execution wasn't success: " + response.toString());
      }
      return response.body().string();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private <T> T blockingExecuteAndParse(Request request, Function<String, T> parser) {
    String response = blockingExecutePlainText(request);
    return parser.apply(response);
  }

}

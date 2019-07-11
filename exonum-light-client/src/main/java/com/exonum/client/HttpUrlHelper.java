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

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.net.URL;
import java.util.Map;
import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;

final class HttpUrlHelper {

  static HttpUrl getFullUrl(URL host, String prefix, String relativeUrl,
      Map<String, String> encodedQueryParameters) {
    checkNotNull(encodedQueryParameters);
    Builder urlBuilder = urlHostBuilder(host)
        .addPathSegments(prefix)
        .addPathSegments(relativeUrl);
    encodedQueryParameters.forEach(urlBuilder::addEncodedQueryParameter);

    return normalize(urlBuilder.build());
  }

  private static HttpUrl.Builder urlHostBuilder(URL host) {
    checkNotNull(host);
    HttpUrl.Builder builder = new HttpUrl.Builder()
        .scheme(host.getProtocol())
        .host(host.getHost());
    if (host.getPort() != -1) {
      builder.port(host.getPort());
    }
    return builder;
  }

  /**
   * Normalized the given URL to the canonical form.
   * Doesn't modify letters case in the URL.
   * Also see {@link URI#normalize()}.
   */
  private static HttpUrl normalize(HttpUrl url) {
    URI normalized = url.uri().normalize();
    return HttpUrl.get(normalized);
  }

  private HttpUrlHelper() {
  }
}

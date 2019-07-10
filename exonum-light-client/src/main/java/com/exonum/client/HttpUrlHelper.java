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

import static java.util.Objects.requireNonNull;

import java.net.URL;
import java.util.Map;
import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;

final class HttpUrlHelper {

  static HttpUrl getFullUrl(URL host, String prefix, String relativeUrl,
      Map<String, String> query) {
    requireNonNull(query);
    Builder builder = urlHostBuilder(host)
        .addPathSegments(normalize(prefix))
        .addPathSegments(normalize(relativeUrl));
    query.forEach(builder::addEncodedQueryParameter);

    return builder.build();
  }

  private static HttpUrl.Builder urlHostBuilder(URL host) {
    requireNonNull(host);
    HttpUrl.Builder builder = new HttpUrl.Builder()
        .scheme(host.getProtocol())
        .host(host.getHost());
    if (host.getPort() != -1) {
      builder.port(host.getPort());
    }
    return builder;
  }

  /**
   * Removes heading slash from the path.
   * Useful because underlying OkHttp applies slashes when constructing paths.
   */
  private static String normalize(String path) {
    requireNonNull(path);
    if (path.startsWith("/")) {
      return path.substring(1);
    } else {
      return path;
    }
  }

  private HttpUrlHelper() {
  }
}

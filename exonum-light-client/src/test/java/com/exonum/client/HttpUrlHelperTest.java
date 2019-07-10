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

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.of;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import okhttp3.HttpUrl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HttpUrlHelperTest {

  @ParameterizedTest
  @MethodSource("source")
  void getFullUrl(String expectedUrl,
      String host, String prefix, String path, Map<String, String> query)
      throws MalformedURLException {
    HttpUrl url = HttpUrlHelper.getFullUrl(new URL(host), prefix, path, query);

    assertThat(url.toString(), is(expectedUrl));
  }

  /**
   * Provides a combination of parameters by the following rules:
   * - port is optional
   * - prefix is optional
   * - paths can start either with or without heading slash
   * - query params is optional.
   */
  private static List<Arguments> source() {
    Map<String, String> noQuery = emptyMap();
    return ImmutableList.of(
        of("http://localhost/path/to/source",
            "http://localhost", "", "/path/to/source", noQuery),
        of("http://localhost/prefix/path/to/source",
            "http://localhost", "prefix", "path/to/source", noQuery),
        of("http://localhost:8080/prefix/path/to/source",
            "http://localhost:8080", "/prefix", "/path/to/source", noQuery),
        of("http://localhost:8080/pre/fix/path/to/source",
            "http://localhost:8080", "/pre/fix", "/path/to/source", noQuery),
        of("http://localhost:8080/pre/fix/path/to/source?key=value",
            "http://localhost:8080", "/pre/fix", "/path/to/source", ImmutableMap.of("key", "value"))
    );
  }

}

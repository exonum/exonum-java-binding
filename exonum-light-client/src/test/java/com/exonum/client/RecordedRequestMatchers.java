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

import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

class RecordedRequestMatchers {

  static Matcher<RecordedRequest> hasPath(String expectedPath) {
    checkPathPrefix(expectedPath);
    Matcher<String> pathMatcher = equalTo("/" + expectedPath);
    return createRequestPathMatcher(pathMatcher);
  }

  static Matcher<RecordedRequest> hasPathStartingWith(String expectedPathPrefix) {
    checkPathPrefix(expectedPathPrefix);
    Matcher<String> pathMatcher = startsWith("/" + expectedPathPrefix);
    return createRequestPathMatcher(pathMatcher);
  }

  private static void checkPathPrefix(String expectedPath) {
    checkArgument(!expectedPath.startsWith("/"), "expectedPath (%s) must not have leading slash",
        expectedPath);
  }

  private static Matcher<RecordedRequest> createRequestPathMatcher(
      Matcher<? super String> pathMatcher) {
    return new FeatureMatcher<RecordedRequest, String>(pathMatcher, "full request path",
        "request path") {

      @Override
      protected String featureValueOf(RecordedRequest actual) {
        return actual.getPath();
      }
    };
  }

  private RecordedRequestMatchers() {}
}

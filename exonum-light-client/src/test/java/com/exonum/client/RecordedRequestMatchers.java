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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import java.util.Set;
import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.Description;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

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

  static Matcher<RecordedRequest> hasNoQueryParam(String queryKey) {
    Matcher<Iterable<? extends String>> keyMatcher = not(contains(queryKey));

    return createRequestQueryKeysMatcher(keyMatcher);
  }

  static Matcher<RecordedRequest> hasQueryParam(String queryKey, Object queryValue) {

    return hasQueryParam(queryKey, String.valueOf(queryValue));
  }

  static Matcher<RecordedRequest> hasQueryParam(String queryKey, String queryValue) {
    Matcher<String> valueMather = equalTo(queryValue);

    return createRequestQueryMatcher(queryKey, valueMather);
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
        return actual.getRequestUrl().encodedPath();
      }
    };
  }

  private static Matcher<RecordedRequest> createRequestQueryKeysMatcher(
      Matcher<Iterable<? extends String>> keysMatcher) {
    return new FeatureMatcher<RecordedRequest, Set<String>>(keysMatcher, "query keys",
        "query keys") {

      @Override
      protected Set<String> featureValueOf(RecordedRequest actual) {
        return actual.getRequestUrl().queryParameterNames();
      }
    };
  }

  private static Matcher<RecordedRequest> createRequestQueryMatcher(String key,
      Matcher<? super String> valueMatcher) {
    return new RequestQueryParamMatcher(key, valueMatcher);
  }

  private static final class RequestQueryParamMatcher extends TypeSafeMatcher<RecordedRequest> {
    private final String key;
    private final Matcher<? super String> valueMatcher;

    RequestQueryParamMatcher(String key,
        Matcher<? super String> valueMatcher) {
      this.key = key;
      this.valueMatcher = valueMatcher;
    }

    @Override
    protected boolean matchesSafely(RecordedRequest actual) {
      return valueMatcher.matches(actual.getRequestUrl().queryParameter(key));
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("query containing [")
          .appendText(key)
          .appendText("->")
          .appendDescriptionOf(valueMatcher)
          .appendText("]");
    }
  }

  private RecordedRequestMatchers() {
  }
}

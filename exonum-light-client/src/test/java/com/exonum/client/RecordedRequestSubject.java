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

import static com.google.common.truth.Truth.assertAbout;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.StringSubject;
import com.google.common.truth.Subject;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.RecordedRequest;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

final class RecordedRequestSubject extends Subject {

  @NullableDecl
  private final RecordedRequest actual;

  private RecordedRequestSubject(FailureMetadata metadata, @NullableDecl RecordedRequest actual) {
    super(metadata, actual);
    this.actual = actual;
  }

  static Factory<RecordedRequestSubject, RecordedRequest> requests() {
    return RecordedRequestSubject::new;
  }

  static RecordedRequestSubject assertThat(RecordedRequest actual) {
    return assertAbout(requests()).that(actual);
  }

  void hasMethod(String method) {
    check("getMethod()").that(actual.getMethod()).isEqualTo(method);
  }

  StringSubject hasUrlQueryParameter(String name) {
    HttpUrl requestUrl = actual.getRequestUrl();
    return check("queryParameter(%s)", name).that(requestUrl.queryParameter(name));
  }
}

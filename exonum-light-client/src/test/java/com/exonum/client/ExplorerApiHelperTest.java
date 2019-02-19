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

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.exonum.binding.common.hash.HashCode;
import org.junit.jupiter.api.Test;

class ExplorerApiHelperTest {

  @Test
  void submitTxBody() {
    String msg = "some data";
    String json = ExplorerApiHelper.submitTxBody(msg);

    assertThat(json, isJson(withJsonPath("$.tx_body", equalTo(msg))));
  }

  @Test
  void submitTxParser() {
    String expected = "f128c720e04b8243";
    String json = "{\"tx_hash\":\"" + expected + "\"}";

    HashCode actual = ExplorerApiHelper.submitTxParser(json);
    assertThat(actual, equalTo(HashCode.fromString(expected)));
  }
}

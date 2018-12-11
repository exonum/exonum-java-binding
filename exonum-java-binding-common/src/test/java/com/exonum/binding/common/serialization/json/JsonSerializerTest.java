/*
 * Copyright 2018 The Exonum Team
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

package com.exonum.binding.common.serialization.json;

import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static com.exonum.binding.test.Bytes.bytes;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import org.junit.jupiter.api.Test;

class JsonSerializerTest {

  @Test
  void longSerializesAsText() {
    long value = 10L;

    String json = json().toJson(new Wrapper<>(value));

    assertJson(json, String.valueOf(value));
  }

  @Test
  void publicKeySerializesAsValue() {
    PublicKey value = PublicKey.fromBytes(bytes(0x00, 0x01, 0x02));

    String json = json().toJson(new Wrapper<>(value));

    assertJson(json, value.toString());
  }

  @Test
  void hashCodeSerializesAsValue() {
    HashCode value = HashCode.fromBytes(bytes(0x00, 0x01, 0x02));

    String json = json().toJson(new Wrapper<>(value));

    assertJson(json, value.toString());
  }

  private static void assertJson(String json, Object expectedValue) {
    assertThat(json, isJson(withJsonPath("$.value", equalTo(expectedValue))));
  }

  private static class Wrapper<T> {
    T value;

    Wrapper(T value) {
      this.value = value;
    }
  }

}

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
import static com.exonum.binding.test.Bytes.fromHex;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import org.junit.jupiter.api.Test;

class JsonSerializerTest {

  @Test
  void longSerializesAsText() {
    long value = 10L;

    String json = json().toJson(new Wrapper<>(value));

    assertJsonValue(json, "10");
  }

  @Test
  void publicKeySerializesAsHexValue() {
    PublicKey value = PublicKey.fromBytes(bytes(0x00, 0x01, 0x02));

    String json = json().toJson(new Wrapper<>(value));

    assertJsonValue(json, "000102");
  }

  @Test
  void hashCodeSerializesAsValue() {
    HashCode value = HashCode.fromBytes(bytes(0x00, 0x01, 0x02));

    String json = json().toJson(new Wrapper<>(value));

    assertJsonValue(json, "000102");
  }

  @Test
  void transactionMessageSerializesAsValue() {
    String hex = "87095f09d413633626a4a5b903d7e8dbb7a9f54baa92eb77965c5ad0417d8d65000"
        + "000007f0000010292a0d0ed9368a70984098519cef6bde555e85eaf8105c561a6c0b9599fae"
        + "f4eb7b02155160f0c2598c97c42bc294599a2be34ce9ce66ba7baa11bfdaf06e5e0c";
    TransactionMessage value = TransactionMessage.fromBytes(fromHex(hex));

    String json = json().toJson(new Wrapper<>(value));

    assertJsonValue(json, hex);
  }

  private static void assertJsonValue(String json, Object expectedValue) {
    assertThat(json, isJson(withJsonPath("$.value", equalTo(expectedValue))));
  }

  private static class Wrapper<T> {
    T value;

    Wrapper(T value) {
      this.value = value;
    }
  }

}

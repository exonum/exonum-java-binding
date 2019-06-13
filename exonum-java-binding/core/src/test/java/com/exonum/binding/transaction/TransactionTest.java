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

package com.exonum.binding.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TransactionTest {

  @Test
  void infoIsEmptyByDefault() {
    Transaction tx = context -> {
      // no-op
    };

    String info = tx.info();

    // Check it is correctly deserialized to an empty object
    Gson gson = new Gson();
    Map<String, ?> deserialized = gson.fromJson(info, new TypeToken<Map<String, ?>>() {
    }.getType());
    assertThat(deserialized).isEmpty();
  }
}

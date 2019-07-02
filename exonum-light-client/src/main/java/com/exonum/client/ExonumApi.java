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

import com.exonum.binding.common.serialization.json.JsonSerializer;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;

final class ExonumApi {
  /**
   * The Gson instance configured to (de)serialize Exonum responses.
   */
  static final Gson JSON = JsonSerializer.builder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .create();

  /**
   * The maximum allowed blocks count per the request.
   */
  static final int MAX_BLOCKS_PER_REQUEST = 1000;


  private ExonumApi() {
  }
}

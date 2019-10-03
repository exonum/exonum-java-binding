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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.exonum.client.response.BlockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExonumHttpClientSysTest {

  private ExonumClient exonumClient;

  @BeforeEach
  void setUp() {
    exonumClient = ExonumClient.newBuilder()
        .setExonumHost("http://127.0.0.1:8000")
        .build();
  }

  @Test
  void getBlockByHeight() {
    BlockResponse blockByHeight = exonumClient.getBlockByHeight(1);

    assertThat(blockByHeight.getBlock().getHeight(), equalTo(1L));
  }
}
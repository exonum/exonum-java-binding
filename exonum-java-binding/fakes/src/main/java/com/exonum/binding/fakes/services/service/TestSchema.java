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
 */

package com.exonum.binding.fakes.services.service;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.service.Schema;
import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class TestSchema implements Schema {
  @SuppressWarnings("WeakerAccess")
  static final String TEST_MAP_NAME = TestService.NAME + "_test_map";
  @SuppressWarnings("WeakerAccess")
  static final String INIT_SERVICE_MAP_NAME = TestService.NAME + "_init_map";
  @SuppressWarnings("WeakerAccess")
  static final String BEFORE_COMMIT_MAP_NAME = TestService.NAME + "_before_commit_map";

  private final View view;

  public TestSchema(View view) {
    this.view = view;
  }

  public ProofMapIndexProxy<HashCode, String> testMap() {
    return ProofMapIndexProxy.newInstance(TEST_MAP_NAME, view, StandardSerializers.hash(),
            StandardSerializers.string());
  }

  public ProofMapIndexProxy<HashCode, String> initializeServiceMap() {
    return ProofMapIndexProxy.newInstance(INIT_SERVICE_MAP_NAME, view, StandardSerializers.hash(),
        StandardSerializers.string());
  }

  public ProofMapIndexProxy<HashCode, String> beforeCommitMap() {
    return ProofMapIndexProxy.newInstance(BEFORE_COMMIT_MAP_NAME, view, StandardSerializers.hash(),
            StandardSerializers.string());
  }

  @Override
  public List<HashCode> getStateHashes() {
    // `8c1ea14c7893acabde2aa95031fae57abb91516ddb78b0f6622afa0d8cb1b5c2` after init
    HashCode initMapHash = initializeServiceMap().getIndexHash();
    // `7324b5c72b51bb5d4c180f1109cfd347b60473882145841c39f3e584576296f9` after init
    HashCode testMapHash = testMap().getIndexHash();

    // exclude beforeCommitMap
    return Arrays.asList(new HashCode[] {initMapHash, testMapHash});
  }
}

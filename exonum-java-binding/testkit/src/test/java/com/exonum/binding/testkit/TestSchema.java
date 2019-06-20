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

package com.exonum.binding.testkit;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.service.Schema;
import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import java.util.Collections;
import java.util.List;

final class TestSchema implements Schema {

  static final String TEST_MAP_NAME = "TestKitService_map";

  private final View view;

  TestSchema(View view) {
    this.view = view;
  }

  ProofMapIndexProxy<HashCode, String> testMap() {
    return ProofMapIndexProxy.newInstance(TEST_MAP_NAME, view, StandardSerializers.hash(),
        StandardSerializers.string());
  }

  @Override
  public List<HashCode> getStateHashes() {
    HashCode rootHash = testMap().getRootHash();
    return Collections.singletonList(rootHash);
  }
}

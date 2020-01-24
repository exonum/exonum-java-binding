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
import com.exonum.binding.core.storage.database.Access;
import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;

final class TestSchema implements Schema {

  private final String testMapName;

  private final Access access;

  TestSchema(Access access, int serviceInstanceId) {
    this.access = access;
    this.testMapName = "TestKitService_map." + serviceInstanceId;
  }

  ProofMapIndexProxy<HashCode, String> testMap() {
    return access
        .getProofMap(IndexAddress.valueOf(testMapName),
            StandardSerializers.hash(), StandardSerializers.string());
  }
}

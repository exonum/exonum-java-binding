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

package com.exonum.binding.fakeservice;

import static com.exonum.binding.common.serialization.StandardSerializers.string;

import com.exonum.binding.core.service.Schema;
import com.exonum.binding.core.storage.database.Access;
import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;

public final class FakeSchema implements Schema {

  private final String namespace;
  private final Access access;

  public FakeSchema(String serviceName, Access access) {
    this.namespace = serviceName;
    this.access = access;
  }

  public ProofMapIndexProxy<String, String> testMap() {
    String fullName = namespace + ".test-map";
    return access.getProofMap(IndexAddress.valueOf(fullName), string(), string());
  }
}

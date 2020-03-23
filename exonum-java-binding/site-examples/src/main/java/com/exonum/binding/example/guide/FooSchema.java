/*
 * Copyright 2020 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.example.guide;

import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.service.Schema;
import com.exonum.binding.core.storage.database.Prefixed;
import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;

@SuppressWarnings("unused") // Example code
final class FooSchema implements Schema {

  private final Prefixed view;

  /**
   * Creates a schema given a prefixed database access object.
   * The database state is determined by the access object.
   */
  FooSchema(Prefixed access) {
    this.view = access;
  }

  /**
   * Provides access to a test ProofMap.
   *
   * <p>Such factory methods may be used in transactions and read requests
   * to access a collection of a certain type and name. Here,
   * a ProofMap with String keys and values is created with a full name
   * {@code "<service name>.test-map"}.
   */
  ProofMapIndexProxy<String, String> testMap() {
    var address = IndexAddress.valueOf("test-map");
    var stringSerializer = StandardSerializers.string();
    return view.getProofMap(address, stringSerializer, stringSerializer);
  }
}

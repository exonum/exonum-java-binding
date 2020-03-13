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
import com.exonum.binding.core.storage.database.Prefixed;
import com.exonum.binding.core.storage.indices.IndexAddress;

@SuppressWarnings("unused") // Used in the examples
class ProofMapCreation {

  void putEntry(Prefixed access, String key, String value) {
    // Access the 'entries' ProofMap
    var indexAddress = IndexAddress.valueOf("entries");
    var stringSerializer = StandardSerializers.string();
    var entries = access.getProofMap(indexAddress, stringSerializer,
        stringSerializer);
    // Put the key-value pair in the ProofMap index
    entries.put(key, value);
  }
}

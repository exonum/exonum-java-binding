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

package com.exonum.binding.core.storage.indices;

import static com.exonum.binding.common.hash.Hashing.DEFAULT_HASH_SIZE_BYTES;
import static com.exonum.binding.core.storage.indices.TestStorageItems.V1;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.core.storage.database.Access;
import org.junit.jupiter.api.Test;

public class ProofEntryIndexProxyIntegrationTest
    extends BaseEntryIndexProxyIntegrationTest<ProofEntryIndex<String>> {

  @Test
  void getIndexHashEmptyEntry() {
    runTestWithView(database::createSnapshot, e -> {
      HashCode indexHash = e.getIndexHash();
      // Expected hash of an empty Entry: all zeroes
      HashCode expectedHash = HashCode.fromBytes(new byte[DEFAULT_HASH_SIZE_BYTES]);
      assertThat(indexHash, equalTo(expectedHash));
    });
  }

  @Test
  void getIndexHashNonEmptyEntry() {
    runTestWithView(database::createFork, e -> {
      String value = V1;
      e.set(value);

      HashCode indexHash = e.getIndexHash();
      // Expected hash of a set Entry: SHA-256(value)
      byte[] valueAsBytes = SERIALIZER.toBytes(value);
      HashCode expectedHash = Hashing.sha256()
          .hashBytes(valueAsBytes);
      assertThat(indexHash, equalTo(expectedHash));
    });
  }

  @Override
  ProofEntryIndexProxy<String> create(IndexAddress address, Access access) {
    return access.getProofEntry(address, SERIALIZER);
  }
}

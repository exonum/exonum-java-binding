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

package com.exonum.binding.core.storage.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.core.storage.indices.IndexAddress;
import com.exonum.binding.core.storage.indices.StorageIndex;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OpenIndexRegistryTest {

  OpenIndexRegistry registry = new OpenIndexRegistry();

  @Nested
  class WithSingleIndex {

    private final IndexAddress address = IndexAddress.valueOf("name");
    private final StorageIndex index = mock(StorageIndex.class, "index 1");

    @BeforeEach
    void registerIndex() {
      when(index.getAddress()).thenReturn(address);

      registry.registerIndex(index);
    }

    @Test
    void canFindRegisteredIndex() {
      Optional<StorageIndex> actual = registry.findIndex(address);

      assertThat(actual).hasValue(index);
    }

    @Test
    void registerThrowsIfAlreadyRegisteredSameAddress() {
      StorageIndex otherIndex = mock(StorageIndex.class, "other index");
      when(otherIndex.getAddress()).thenReturn(address);

      Exception e =
          assertThrows(IllegalArgumentException.class, () -> registry.registerIndex(otherIndex));

      String message = e.getMessage();
      assertThat(message)
          .contains(String.valueOf(address))
          .contains(String.valueOf(index))
          .contains(String.valueOf(otherIndex));
    }

    @Test
    void clearRemovesTheIndex() {
      registry.clear();

      Optional<StorageIndex> actual = registry.findIndex(address);

      assertThat(actual).isEmpty();
    }
  }

  @Test
  void findUnknownIndex() {
    IndexAddress unknownAddress = IndexAddress.valueOf("Unknown");
    Optional<StorageIndex> index = registry.findIndex(unknownAddress);

    assertThat(index).isEmpty();
  }
}

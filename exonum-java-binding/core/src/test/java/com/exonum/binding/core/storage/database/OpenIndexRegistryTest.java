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

import com.exonum.binding.core.storage.indices.StorageIndex;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OpenIndexRegistryTest {

  OpenIndexRegistry registry = new OpenIndexRegistry();

  @Nested
  class WithSingleIndex {

    private final Long id = 1L;
    private final StorageIndex index = mock(StorageIndex.class, "index 1");

    @BeforeEach
    void registerIndex() {
      registry.registerIndex(id, index);
    }

    @Test
    void canFindRegisteredIndex() {
      Optional<StorageIndex> actual = registry.findIndex(id);

      assertThat(actual).hasValue(index);
    }

    @Test
    void registerThrowsIfAlreadyRegisteredSameId() {
      StorageIndex otherIndex = mock(StorageIndex.class, "other index");

      Exception e = assertThrows(IllegalArgumentException.class,
          () -> registry.registerIndex(id, otherIndex));

      String message = e.getMessage();
      assertThat(message).contains(String.valueOf(id))
          .contains(String.valueOf(index))
          .contains(String.valueOf(otherIndex));
    }

    @Test
    void clearRemovesTheIndex() {
      registry.clear();

      Optional<StorageIndex> actual = registry.findIndex(id);

      assertThat(actual).isEmpty();
    }
  }

  @Test
  void findUnknownIndex() {
    long unknownId = 1024L;
    Optional<StorageIndex> index = registry.findIndex(unknownId);

    assertThat(index).isEmpty();
  }
}

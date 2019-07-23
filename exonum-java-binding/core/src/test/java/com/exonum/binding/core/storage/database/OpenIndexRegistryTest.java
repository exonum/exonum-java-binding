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

import com.exonum.binding.core.storage.indices.IndexAddress;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OpenIndexRegistryTest {

  OpenIndexRegistry registry = new OpenIndexRegistry();

  @Nested
  class WithSingleIndex {

    private final IndexAddress address = new IndexAddress("name");
    private final String entry = "John";

    @BeforeEach
    void registerIndex() {
      registry.registerIndex(address, entry);
    }

    @Test
    void canFindRegisteredIndex() {
      Optional<String> index = registry.findIndex(address, String.class);

      assertThat(index).hasValue(entry);
    }

    @Test
    void findThrowsIfWrongType() {
      // todo: Shall we really do this in the registry, or in View?
      Class<?> requestedType = List.class;
      Exception e = assertThrows(ClassCastException.class,
          () -> registry.findIndex(address, requestedType));

      String message = e.getMessage();
      assertThat(message).contains(requestedType.getSimpleName())
          .contains("String");
    }

    @Test
    void registerThrowsIfAlreadyRegistered() {
      String otherEntry = "Daniel";
      Exception e = assertThrows(
          IllegalArgumentException.class,
          () -> registry.registerIndex(address, otherEntry));

      String message = e.getMessage();
      assertThat(message).contains(String.valueOf(address))
          .contains(entry)
          .contains(otherEntry);
    }
  }

  @Test
  void findUnknownIndex() {
    IndexAddress unknownAddress = new IndexAddress("Unknown");
    Optional<String> index = registry.findIndex(unknownAddress, String.class);

    assertThat(index).isEmpty();
  }
}

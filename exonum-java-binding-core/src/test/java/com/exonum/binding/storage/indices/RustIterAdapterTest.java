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

package com.exonum.binding.storage.indices;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class RustIterAdapterTest {

  private RustIterAdapter<Integer> adapter;

  @Test
  void nextThrowsIfNoNextItem0() {
    adapter = new RustIterAdapter<>(
        new RustIterTestFake(emptyList()));

    assertThrows(NoSuchElementException.class, () -> adapter.next());
  }

  @Test
  void nextThrowsIfNoNextItem1() {
    adapter = new RustIterAdapter<>(
        new RustIterTestFake(singletonList(1)));

    adapter.next();

    assertThrows(NoSuchElementException.class, () -> adapter.next());
  }
}

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

package com.exonum.binding.core.storage.indices;

import java.util.Iterator;
import java.util.Optional;

/**
 * A simple RustIter fake with no native code.
 *
 * <p>An adapter of {@link Iterator} to {@link RustIter}.
 */
public class RustIterTestFake implements RustIter<Integer> {
  private final Iterator<Integer> iterator;

  RustIterTestFake(Iterable<Integer> iterable) {
    this.iterator = iterable.iterator();
  }

  @Override
  public Optional<Integer> next() {
    return iterator.hasNext() ? Optional.of(iterator.next())
                              : Optional.empty();
  }
}

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

package com.exonum.client;

import com.google.common.collect.Iterables;
import java.util.OptionalInt;
import java.util.function.Predicate;

final class ExonumIterables {

  /**
   * Returns an index of the first element matching the predicate or {@code OptionalInt.empty()}
   * if no such element exists.
   *
   * @param list a list to search in
   * @param p a predicate that an element must match
   * @param <T> the type of elements
   */
  static <T> OptionalInt indexOf(Iterable<T> list, Predicate<? super T> p) {
    int i = Iterables.indexOf(list, p::test);
    if (i == -1) {
      return OptionalInt.empty();
    }
    return OptionalInt.of(i);
  }

  private ExonumIterables() {}
}

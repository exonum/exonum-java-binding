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

package com.exonum.binding.core.proxy;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class FrequencyStatsFormatter {

  static <ElementT, KeyT>
      String itemsFrequency(Collection<? extends ElementT> items,
                            Function<? super ElementT, KeyT> keyExtractor) {

    Map<KeyT, Long> numItemsByType = items.stream()
        .collect(groupingBy(keyExtractor, counting()));

    String itemsFrequency = numItemsByType.entrySet().stream()
        .sorted(Comparator.<Map.Entry<KeyT, Long>>
            comparingLong(Map.Entry::getValue).reversed())
        .map(Object::toString)
        .collect(Collectors.joining(", "));

    return "{" + itemsFrequency + "}";
  }

  private FrequencyStatsFormatter() {}
}

package com.exonum.binding.proxy;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class FrequencyStatsFormatter {

  static <ElementT, KeyT>
      String itemsFrequency(Collection<ElementT> items,
                            Function<? super ElementT, ? extends KeyT> keyExtractor) {

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
